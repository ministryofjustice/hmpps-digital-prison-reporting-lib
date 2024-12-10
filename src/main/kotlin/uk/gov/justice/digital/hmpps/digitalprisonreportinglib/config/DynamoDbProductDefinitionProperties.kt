package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("dpr.lib.dataproductdefinitions.dynamodb")
class DynamoDbProductDefinitionProperties(
  var region: String = "eu-west-2",
  var accountId: String = "771283872747",
  var tableName: String = "dpr-data-product-definition",
  var categoryFieldName: String = "category",
  var definitionFieldName: String = "definition",
  var categoryIndexName: String = "category-index",
) {
  val tableArn: String = "arn:aws:dynamodb:${region}:${accountId}:table/${tableName}"
}
