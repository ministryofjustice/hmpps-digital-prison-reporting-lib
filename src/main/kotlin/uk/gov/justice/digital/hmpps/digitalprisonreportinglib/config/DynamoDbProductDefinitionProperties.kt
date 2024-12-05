package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("dpr.lib.dataproductdefinitions.dynamodb")
class DynamoDbProductDefinitionProperties(
  val tableName: String = "dpr-data-product-definition",
  val categoryFieldName: String = "category",
  val definitionFieldName: String = "definition",
  val categoryIndexName: String = "category-index",
)