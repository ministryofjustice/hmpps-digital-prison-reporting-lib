package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.gson.Gson
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

class DynamoDbProductDefinitionRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: AwsProperties,
  private val gson: Gson,
  private val definitionsCache: Cache<String, List<ProductDefinition>>?,
) : AbstractProductDefinitionRepository() {
  companion object {
    val defaultPath = "definitions/prisons/orphanage"

    fun getQueryRequest(properties: AwsProperties, path: String): QueryRequest {
      val attrValues: Map<String, AttributeValue> = mapOf(":${properties.dynamoDb.categoryFieldName}" to AttributeValue.fromS(path))

      return QueryRequest.builder()
        .tableName(properties.dynamoDbTableArn)
        .indexName(properties.dynamoDb.categoryIndexName)
        .keyConditionExpression("${properties.dynamoDb.categoryFieldName} = :${properties.dynamoDb.categoryFieldName}")
        .expressionAttributeValues(attrValues)
        .build()
    }
  }

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    val usePath = path ?: defaultPath

    val cachedDefinitions = definitionsCache?.let { cache ->
      path?.let { path -> cache.getIfPresent(path) }
    }
    cachedDefinitions?.let { return it }

    val definitions = dynamoDbClient
      .query(getQueryRequest(properties, usePath))
      .items()
      ?.filter { it[properties.dynamoDb.definitionFieldName] != null }
      ?.map { gson.fromJson(it[properties.dynamoDb.definitionFieldName]!!.s(), ProductDefinition::class.java) }

    return definitions?.let { responseBody ->
      definitionsCache?.put(usePath, responseBody)
      responseBody
    } ?: emptyList()
  }
}
