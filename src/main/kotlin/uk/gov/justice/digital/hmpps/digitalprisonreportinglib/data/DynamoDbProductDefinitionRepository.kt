package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

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
    return dynamoDbClient
      .query(getQueryRequest(properties, path ?: defaultPath))
      .items()
      ?.map { gson.fromJson(it[properties.dynamoDb.definitionFieldName]!!.s(), ProductDefinition::class.java) }
      ?: emptyList()
  }
}
