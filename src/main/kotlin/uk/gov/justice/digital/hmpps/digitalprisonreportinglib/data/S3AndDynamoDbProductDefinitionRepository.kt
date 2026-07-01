package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.gson.Gson
import jakarta.validation.ValidationException
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.LoadedDefinitions
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService.Companion.INVALID_REPORT_ID_MESSAGE

class S3AndDynamoDbProductDefinitionRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val s3Client: S3Client,
  private val properties: AwsProperties,
  private val s3Bucket: String,
  private val gson: Gson,
  private val s3AndDdbDefinitionsCache: Cache<String, LoadedDefinitions>? = null,
  identifiedHelper: IdentifiedHelper,
) : AbstractProductDefinitionRepository(identifiedHelper) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val CACHE_KEY = "all-product-definitions"
    private const val DDB_ID_PREFIX = "dpr_"
  }

  override fun getProductDefinitions(path: String?): List<ProductDefinitionSummary> {
    val stopWatch = StopWatch.createStarted()

    val loadedDefinitions = loadDefinitionsFromCache()

    stopWatch.stop()
    log.debug("Combined product definition retrieval took: {} ms.", stopWatch.time)

    return loadedDefinitions.summaries
  }

  override fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String?): ProductDefinition {
    val stopwatch = StopWatch.createStarted()

    val loadedDefinitions = loadDefinitionsFromCache()

    val definition = loadedDefinitions.definitionsById[definitionId]
      ?: throw ValidationException("$INVALID_REPORT_ID_MESSAGE $definitionId")

    stopwatch.stop()
    log.debug("Getting product definition for {} took overall: {}", definitionId, stopwatch.time)

    return definition
  }

  private fun loadDefinitionsFromCache(): LoadedDefinitions = s3AndDdbDefinitionsCache?.get(CACHE_KEY) {
    loadProductDefinitionsWithSummaries()
  } ?: loadProductDefinitionsWithSummaries()

  private fun loadProductDefinitionsWithSummaries(): LoadedDefinitions {
    log.debug("Loading product definitions from DynamoDB orphanage and S3")

    val ddbDefinitions = loadDynamoDbOrphanageDefinitions()
    val s3Definitions = loadAllS3Definitions()

    return buildCombinedDefinitions(ddbDefinitions + s3Definitions)
  }

  private fun loadDynamoDbOrphanageDefinitions(): List<LoadedDefinitionWithSummary> {
    val path = DataDefinitionPath.ORPHANAGE.value

    log.debug("Loading DynamoDB definitions for path: {}", path)

    val request = DynamoDbProductDefinitionRepository.getQueryRequest(properties, path)
    val paginator = dynamoDbClient.queryPaginator(request)

    return paginator.items().mapNotNull { item ->
      try {
        val json = item[properties.dynamoDb.definitionFieldName]!!.s()
        val originalSummary = gson.fromJson(json, ProductDefinitionSummary::class.java)
        val originalDefinition = gson.fromJson(json, ProductDefinition::class.java)
        val globalId = "$DDB_ID_PREFIX${originalSummary.id}"

        LoadedDefinitionWithSummary(
          summary = originalSummary.copy(
            id = globalId,
            // path is not set in any of our DPDs but making sure here even if it was set that
            // it will be null so we can remove it from our model later
            path = null,
          ),
          definition = originalDefinition.copy(
            id = globalId,
            path = null,
          ),
        )
      } catch (ex: Exception) {
        log.warn("Skipping invalid DynamoDB DPD item for path {}", path, ex)
        null
      }
    }.toList()
  }

  private fun loadAllS3Definitions(): List<LoadedDefinitionWithSummary> {
    val teamPrefixes = listTeamPrefixes()

    log.debug("Discovered S3 DPD team prefixes: {}", teamPrefixes)

    return teamPrefixes.flatMap { teamPrefix ->
      loadS3DefinitionsForTeam(teamPrefix)
    }
  }

  private fun listTeamPrefixes(): List<String> {
    val request = ListObjectsV2Request.builder()
      .bucket(s3Bucket)
      .delimiter("/")
      .build()

    return s3Client.listObjectsV2Paginator(request)
      .flatMap { response -> response.commonPrefixes() }
      .map { commonPrefix -> commonPrefix.prefix().removeSuffix("/") }
      .filter { it.isNotBlank() }
      .toList()
  }

  private fun loadS3DefinitionsForTeam(teamPrefix: String): List<LoadedDefinitionWithSummary> {
    val keys = listJsonKeysForTeam(teamPrefix)

    return keys.mapNotNull { key ->
      try {
        val json = getS3ObjectAsString(key)

        val originalSummary = gson.fromJson(json, ProductDefinitionSummary::class.java)
        val originalDefinition = gson.fromJson(json, ProductDefinition::class.java)

        val globalId = "${teamPrefix}_${originalSummary.id}"

        LoadedDefinitionWithSummary(
          summary = originalSummary.copy(
            id = globalId,
            // this is not set in any of our DPDs but making sure here even if it was set that
            // it will be null so we can remove it from our model later
            path = null,
          ),
          definition = originalDefinition.copy(
            id = globalId,
            path = null,
          ),
        )
      } catch (ex: Exception) {
        log.warn(
          "Skipping invalid S3 DPD. bucket={} key={}",
          s3Bucket,
          key,
          ex,
        )
        null
      }
    }
  }

  private fun listJsonKeysForTeam(teamPrefix: String): List<String> {
    val prefix = "$teamPrefix/"

    val request = ListObjectsV2Request.builder()
      .bucket(s3Bucket)
      .prefix(prefix)
      .build()

    return s3Client.listObjectsV2Paginator(request)
      .contents()
      .map { it.key() }
      .filter { it.endsWith(".json") }
      .toList()
  }

  private fun getS3ObjectAsString(key: String): String {
    val request = GetObjectRequest.builder()
      .bucket(s3Bucket)
      .key(key)
      .build()

    val responseBytes = s3Client.getObject(
      request,
      ResponseTransformer.toBytes(),
    )

    return responseBytes.asUtf8String()
  }

  private fun buildCombinedDefinitions(loadedDefinitionWithSummaries: List<LoadedDefinitionWithSummary>): LoadedDefinitions {
    val definitionsById = linkedMapOf<String, ProductDefinition>()
    val summaries = mutableListOf<ProductDefinitionSummary>()

    loadedDefinitionWithSummaries.forEach { loaded ->
      val id = loaded.summary.id

      summaries.add(loaded.summary)
      definitionsById[id] = loaded.definition
    }

    return LoadedDefinitions(
      summaries = summaries.toList(),
      definitionsById = definitionsById.toMap(),
    )
  }
  private data class LoadedDefinitionWithSummary(
    val summary: ProductDefinitionSummary,
    val definition: ProductDefinition,
  )
}
