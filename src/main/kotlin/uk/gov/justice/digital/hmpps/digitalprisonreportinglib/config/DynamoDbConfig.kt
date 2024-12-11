package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider

@Configuration
class DynamoDbConfig {

  @Bean
  @ConditionalOnProperty("dpr.lib.aws.dynamodb.enabled", havingValue = "true")
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  @ConditionalOnMissingBean(DynamoDbClient::class)
  fun dynamoDbClientSts(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): DynamoDbClient =
    DynamoDbClient.builder()
      .region(properties.typedRegion)
      .credentialsProvider(stsAssumeRoleCredentialsProvider)
      .build()

  @Bean
  @ConditionalOnProperty("dpr.lib.aws.dynamodb.enabled", havingValue = "true")
  @ConditionalOnMissingBean(value = [DynamoDbClient::class, StsAssumeRoleCredentialsProvider::class])
  fun dynamoDbClient(
    properties: AwsProperties,
  ): DynamoDbClient =
    DynamoDbClient.builder()
      .region(properties.typedRegion)
      .build()
}
