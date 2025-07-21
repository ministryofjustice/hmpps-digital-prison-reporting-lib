package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.gson.Gson
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

class DynamoDbProductDefinitionRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: AwsProperties,
  private val gson: Gson,
  private val definitionsCache: Cache<String, List<ProductDefinition>>? = null,
  identifiedHelper: IdentifiedHelper,
) : AbstractProductDefinitionRepository(identifiedHelper) {
  companion object {
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

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    val usePaths = mutableListOf(DataDefinitionPath.MISSING.value)
    usePaths.add(if (path?.isEmpty() == false) path else DataDefinitionPath.ORPHANAGE.value)

    val cachedDefinitions = definitionsCache?.let { cache ->
      path?.let { path -> cache.getIfPresent(path) }
    }
    cachedDefinitions?.let { return it }

    var response = dynamoDbClient.scan(getScanRequest(properties, usePaths))
    val items: MutableList<Map<String, AttributeValue>> = mutableListOf()

    while (response.hasLastEvaluatedKey()) {
      items.addAll(response.items())
      response = dynamoDbClient.scan(getScanRequest(properties, usePaths, response.lastEvaluatedKey()))
    }

    items.addAll(response.items())

    val definitionMap = usePaths.associateWith { mutableListOf<ProductDefinition>() }
    items
      .filter { it[properties.dynamoDb.definitionFieldName] != null }
      .forEach {
        val definition = gson.fromJson(it[properties.dynamoDb.definitionFieldName]!!.s(), ProductDefinition::class.java)
        val definitionPath = it[properties.dynamoDb.categoryFieldName]!!.s()
        definition.path = DataDefinitionPath.entries.firstOrNull { path -> path.value == definitionPath } ?: DataDefinitionPath.OTHER
        definitionMap[definitionPath]?.add(definition)
      }

    definitionMap.forEach { definitionsCache?.put(it.key, it.value) }

    return definitionMap.values.flatten()
  }
}
