package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.QueryRequest
import com.google.gson.Gson
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DynamoDbProductDefinitionProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

class DynamoDbProductDefinitionRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: DynamoDbProductDefinitionProperties,
  private val gson: Gson,
) : AbstractProductDefinitionRepository() {
  companion object {
    val defaultPath = "definitions/prisons/orphanage"

    fun getQueryRequest(properties: DynamoDbProductDefinitionProperties, path: String): QueryRequest {
      val attrValues: Map<String, AttributeValue> = mapOf(":${properties.categoryFieldName}" to AttributeValue.S(path))

      return QueryRequest {
        tableName = properties.tableName
        indexName = properties.categoryIndexName
        keyConditionExpression = "${properties.categoryFieldName} = :${properties.categoryFieldName}"
        expressionAttributeValues = attrValues
        attributesToGet = listOf(properties.definitionFieldName)
      }
    }
  }

  override suspend fun getProductDefinitions(path: String?): List<ProductDefinition> {
    return dynamoDbClient.query(getQueryRequest(properties, path ?: defaultPath)).items
      ?.filter { it[properties.definitionFieldName] != null }
      ?.map { gson.fromJson(it[properties.definitionFieldName]!!.asS(), ProductDefinition::class.java) }
      ?: emptyList()
  }
}
