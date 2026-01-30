package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.gson.Gson
import jakarta.validation.ValidationException
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.ScanResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService.Companion.INVALID_REPORT_ID_MESSAGE

class DynamoDbProductDefinitionRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: AwsProperties,
  private val gson: Gson,
  private val definitionsCache: Cache<String, List<ProductDefinitionSummary>>? = null,
  identifiedHelper: IdentifiedHelper,
) : AbstractProductDefinitionRepository(identifiedHelper) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun getScanRequest(properties: AwsProperties, paths: List<String>, exclusiveStartKey: Map<String, AttributeValue>? = null): ScanRequest {
      val attrValues: Map<String, AttributeValue> = mapOf(":${properties.dynamoDb.categoryFieldName}" to AttributeValue.fromSs(paths))

      return ScanRequest.builder()
        .tableName(properties.getDynamoDbTableArn())
        .indexName(properties.dynamoDb.categoryIndexName)
        .filterExpression("contains(:${properties.dynamoDb.categoryFieldName}, ${properties.dynamoDb.categoryFieldName})")
        .expressionAttributeValues(attrValues)
        .exclusiveStartKey(exclusiveStartKey)
        .build()
    }
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
    definition.path = DataDefinitionPath.entries.firstOrNull { path -> path.value == definitionPath } ?: DataDefinitionPath.OTHER
    stopwatch.stop()
    log.debug("Getting product definition for {} {} took overall: {}", definitionId, path, stopwatch.time)
    return definition
  }

  override fun getProductDefinitions(path: String?): List<ProductDefinitionSummary> {
    val overallStopwatch = StopWatch.createStarted()
    val usePaths = mutableListOf(DataDefinitionPath.MISSING.value)
    usePaths.add(if (path?.isEmpty() == false) path else DataDefinitionPath.ORPHANAGE.value)

    val cachedDefinitions = usePaths.map { usePath ->
      definitionsCache?.let { cache ->
        usePath.let { path -> cache.getIfPresent(path) }
      }.orEmpty()
    }
    // Make sure every path has results
    if (cachedDefinitions.all { it.isNotEmpty() }) {
      return cachedDefinitions.flatten()
    }

    val definitionMap = usePaths.associateWith { mutableListOf<ProductDefinitionSummary>() }
    scanAndPopulateDefinitionsMap(usePaths, definitionMap)
    overallStopwatch.stop()
    log.debug("Getting product definitions took overall: {}", overallStopwatch.time)
    return definitionMap.values.flatten()
  }

  private fun scanAndPopulateDefinitionsMap(usePaths: MutableList<String>, definitionMap: Map<String, MutableList<ProductDefinitionSummary>>) {
    val scanStopwatch = StopWatch.createStarted()
    var response = dynamoDbClient.scan(getScanRequest(properties, usePaths))

    while (response.hasLastEvaluatedKey()) {
      addToDefinitionsMap(response, definitionMap)
      response = dynamoDbClient.scan(getScanRequest(properties, usePaths, response.lastEvaluatedKey()))
    }
    scanStopwatch.stop()
    log.debug("DynamoDB scan for product definitions took: {}", scanStopwatch.time)

    addToDefinitionsMap(response, definitionMap)
  }

  private fun addToDefinitionsMap(response: ScanResponse, definitionMap: Map<String, MutableList<ProductDefinitionSummary>>) {
    response.items()
      .filter { it[properties.dynamoDb.definitionFieldName] != null }
      .forEach {
        val deserialisationStopwatch = StopWatch.createStarted()
        val definition =
          gson.fromJson(it[properties.dynamoDb.definitionFieldName]!!.s(), ProductDefinitionSummary::class.java)
        deserialisationStopwatch.stop()
        log.debug("Deserialisation of product definition {} took: {}", definition.id, deserialisationStopwatch.time)
        val definitionPath = it[properties.dynamoDb.categoryFieldName]!!.s()
        definition.path =
          DataDefinitionPath.entries.firstOrNull { path -> path.value == definitionPath } ?: DataDefinitionPath.OTHER
        definitionMap[definitionPath]?.add(definition)
      }
    definitionMap.forEach { definitionsCache?.put(it.key, it.value) }
  }
}
