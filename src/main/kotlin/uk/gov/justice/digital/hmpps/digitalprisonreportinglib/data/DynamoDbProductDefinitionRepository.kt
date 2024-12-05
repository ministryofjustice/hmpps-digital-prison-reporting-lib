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
  }

  override suspend fun getProductDefinitions(path: String?): List<ProductDefinition> {
    // Set up mapping of the partition name with the value.
    val attrValues: Map<String, AttributeValue> = mapOf(":${properties.categoryFieldName}" to AttributeValue.S(path ?: defaultPath))

    val request =
      QueryRequest {
        tableName = properties.tableName
        indexName = properties.categoryIndexName
        keyConditionExpression = "${properties.categoryFieldName} = :${properties.categoryFieldName}"
        expressionAttributeValues = attrValues
        attributesToGet = listOf(properties.definitionFieldName)
      }

    return dynamoDbClient.query(request).items
      ?.filter { it[properties.definitionFieldName] != null }
      ?.map { gson.fromJson(it[properties.definitionFieldName]!!.asS(), ProductDefinition::class.java) }
      ?: emptyList()
  }
}
