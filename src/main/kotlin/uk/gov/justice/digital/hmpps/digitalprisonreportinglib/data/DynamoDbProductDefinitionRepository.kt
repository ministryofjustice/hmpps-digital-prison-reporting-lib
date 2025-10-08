package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.gson.Gson
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.resource.NoResourceFoundException
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
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

    fun getQueryRequest(properties: AwsProperties, paths: List<String>, exclusiveStartKey: Map<String, AttributeValue>? = null): QueryRequest {
      val attrValues: Map<String, AttributeValue> = mapOf(":${properties.dynamoDb.categoryFieldName}" to AttributeValue.fromSs(paths))

      return QueryRequest.builder()
        .tableName(properties.getDynamoDbTableArn())
        .indexName(properties.dynamoDb.categoryIndexName)
        .keyConditionExpression("contains(:${properties.dynamoDb.categoryFieldName}, ${properties.dynamoDb.categoryFieldName})")
        .expressionAttributeValues(attrValues)
        .exclusiveStartKey(exclusiveStartKey)
        .build()
    }
  }

  override fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String?): ProductDefinition {
    val path = if (dataProductDefinitionsPath.isNullOrBlank()) dataProductDefinitionsPath else DataDefinitionPath.ORPHANAGE.value
    val keyMap = hashMapOf<String?, AttributeValue?>(
      "data-product-id" to AttributeValue.builder().s(definitionId).build(),
      "category" to AttributeValue.builder().s(path).build(),
    )
    val getItemRequest = GetItemRequest.builder()
      .tableName(properties.getDynamoDbTableArn())
      .key(keyMap)
      .build()
    val response = dynamoDbClient.getItem(getItemRequest)

    if (!response.hasItem()) {
      throw NoResourceFoundException(HttpMethod.GET, "/definitions/$definitionId")
    }
    val item = response.item()
    val definition = gson.fromJson(item[properties.dynamoDb.definitionFieldName]!!.s(), ProductDefinition::class.java)
    val definitionPath = item[properties.dynamoDb.categoryFieldName]!!.s()
    definition.path = DataDefinitionPath.entries.firstOrNull { path -> path.value == definitionPath } ?: DataDefinitionPath.OTHER
    return definition
  }

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    val usePaths = mutableListOf(DataDefinitionPath.MISSING.value)
    usePaths.add(if (path?.isEmpty() == false) path else DataDefinitionPath.ORPHANAGE.value)

    val cachedDefinitions = usePaths.flatMap { usePath ->
      definitionsCache?.let { cache ->
        usePath.let { path -> cache.getIfPresent(path) }
      }.orEmpty()
    }
    if (cachedDefinitions.isNotEmpty()) {
      return cachedDefinitions
    }

    var response = dynamoDbClient.query(getQueryRequest(properties, usePaths))
    val items: MutableList<Map<String, AttributeValue>> = mutableListOf()

    while (response.hasLastEvaluatedKey()) {
      items.addAll(response.items())
      response = dynamoDbClient.query(getQueryRequest(properties, usePaths, response.lastEvaluatedKey()))
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
