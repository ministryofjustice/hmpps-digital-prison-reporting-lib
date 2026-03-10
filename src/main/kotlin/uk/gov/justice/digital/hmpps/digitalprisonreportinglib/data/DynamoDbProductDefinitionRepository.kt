package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.gson.Gson
import jakarta.validation.ValidationException
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService.Companion.INVALID_REPORT_ID_MESSAGE
import kotlin.collections.emptyList

class DynamoDbProductDefinitionRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: AwsProperties,
  private val gson: Gson,
  private val definitionsCache: Cache<String, List<ProductDefinitionSummary>>? = null,
  identifiedHelper: IdentifiedHelper,
) : AbstractProductDefinitionRepository(identifiedHelper) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val pathLookup = DataDefinitionPath.entries.associateBy { it.value }

    fun getQueryRequest(properties: AwsProperties, path: String): QueryRequest = QueryRequest.builder()
      .tableName(properties.getDynamoDbTableArn())
      .indexName(properties.dynamoDb.categoryIndexName)
      .keyConditionExpression("#cat = :category")
      .expressionAttributeNames(
        mapOf(
          "#cat" to properties.dynamoDb.categoryFieldName,
          "#def" to properties.dynamoDb.definitionFieldName,
        ),
      )
      .expressionAttributeValues(
        mapOf(
          ":category" to AttributeValue.builder().s(path).build(),
        ),
      )
      .projectionExpression("#def, #cat")
      .build()
  }

  override fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String?): ProductDefinition {
    val stopwatch = StopWatch.createStarted()
    val path = if (dataProductDefinitionsPath.isNullOrBlank()) DataDefinitionPath.ORPHANAGE.value else dataProductDefinitionsPath
    val keyMap = hashMapOf<String, AttributeValue>(
      "data-product-id" to AttributeValue.builder().s(definitionId).build(),
      "category" to AttributeValue.builder().s(path).build(),
    )
    val getItemRequest = GetItemRequest.builder()
      .tableName(properties.getDynamoDbTableArn())
      .key(keyMap)
      .build()
    val response = dynamoDbClient.getItem(getItemRequest)
    if (!response.hasItem()) {
      throw ValidationException("$INVALID_REPORT_ID_MESSAGE $definitionId")
    }
    val item = response.item()
    val definition = gson.fromJson(item[properties.dynamoDb.definitionFieldName]!!.s(), ProductDefinition::class.java)
    val definitionPath = item[properties.dynamoDb.categoryFieldName]!!.s()
    definition.path = pathLookup[definitionPath] ?: DataDefinitionPath.OTHER
    stopwatch.stop()
    log.debug("Getting product definition for {} {} took overall: {}", definitionId, path, stopwatch.time)
    return definition
  }

  override fun getProductDefinitions(path: String?): List<ProductDefinitionSummary> {
    val stopWatch = StopWatch.createStarted()
    val requestedPath =
      if (path.isNullOrBlank()) DataDefinitionPath.ORPHANAGE.value else path
    val missingDefs = loadFromCache(DataDefinitionPath.MISSING.value)
    val requestedDefs =
      if (requestedPath == DataDefinitionPath.MISSING.value) {
        emptyList()
      } else {
        loadFromCache(requestedPath)
      }
    log.debug("Definition retrieval took: ${stopWatch.time} ms.")
    return missingDefs + requestedDefs
  }

  private fun loadFromCache(path: String): List<ProductDefinitionSummary> = definitionsCache?.get(path) {
    queryDefinitionsForPath(path)
  } ?: queryDefinitionsForPath(path)

  private fun queryDefinitionsForPath(path: String): List<ProductDefinitionSummary> {
    log.debug("Retrieving definitions from DynamoDB for path: $path")
    val results = mutableListOf<ProductDefinitionSummary>()
    val request = getQueryRequest(properties, path)
    val paginator = dynamoDbClient.queryPaginator(request)
    paginator.items().forEach { item ->
      val json = item[properties.dynamoDb.definitionFieldName]!!.s()
      val definition =
        gson.fromJson(json, ProductDefinitionSummary::class.java)
      val definitionPath = item[properties.dynamoDb.categoryFieldName]!!.s()
      definition.path = pathLookup[definitionPath] ?: DataDefinitionPath.OTHER
      results.add(definition)
    }
    return results
  }
}
