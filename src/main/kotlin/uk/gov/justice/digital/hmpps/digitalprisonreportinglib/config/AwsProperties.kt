package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region

@ConfigurationProperties("dpr.lib.aws")
class AwsProperties(
  var region: String = "eu-west-2",
  var accountId: String = "771283872747",
  var dynamoDb: DynamoDb = DynamoDb(),
  var sts: Sts = Sts(),
) {

  class DynamoDb(
    var tableName: String = "dpr-data-product-definition",
    var categoryFieldName: String = "category",
    var definitionFieldName: String = "definition",
    var categoryIndexName: String = "category-index",
  )

  class Sts(
    var tokenRefreshDurationSec: Int = 3600,
    var roleName: String = "dpr-data-api-cross-account-role",
    var roleSessionName: String = "dpr-cross-account-role-session",
  )

  fun getDynamoDbTableArn(): String = "arn:aws:dynamodb:$region:$accountId:table/${dynamoDb.tableName}"

  fun getStsRoleArn(): String = "arn:aws:iam:$accountId:role/${sts.roleName}"

  fun getRegion(): Region = Region.of(region)
}
