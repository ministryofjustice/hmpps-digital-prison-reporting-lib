package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DynamoDbConfig {

  @Bean
  @ConditionalOnProperty("dpr.lib.dataproductdefinitions.dynamodb.enabled", havingValue = "true")
  @ConditionalOnMissingBean(DynamoDbClient::class)
  fun dynamoDbClient(
    @Value("\${dpr.lib.dataProductDefinitions.dynamoDbTable}") dynamoDbTable: String,
    ): DynamoDbClient {
    return DynamoDbClient.builder()
      .build()
  }
}
