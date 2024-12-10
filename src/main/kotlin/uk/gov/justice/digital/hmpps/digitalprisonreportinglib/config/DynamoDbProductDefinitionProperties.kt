package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("dpr.lib.dataproductdefinitions.dynamodb")
class DynamoDbProductDefinitionProperties(
  val region: String = "eu-west-2",
  val accountId: String = "771283872747",
  val tableName: String = "dpr-data-product-definition",
  val categoryFieldName: String = "category",
  val definitionFieldName: String = "definition",
  val categoryIndexName: String = "category-index",
) {
  val tableArn: String = "arn:aws:dynamodb:${this.region}:${this.accountId}:table/${this.tableName}"
}
