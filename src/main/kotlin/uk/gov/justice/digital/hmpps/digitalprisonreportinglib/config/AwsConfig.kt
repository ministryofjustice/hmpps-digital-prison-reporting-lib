package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest

@Configuration
class AwsConfig {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  @ConditionalOnProperty("dpr.lib.aws.sts.enabled", havingValue = "true")
  fun stsAssumeRoleCredentialsProvider(properties: AwsProperties): StsAssumeRoleCredentialsProvider {
    log.debug("AWS properties: {}", properties)

    val stsClient: StsClient = StsClient.builder()
      .region(properties.getRegion())
      .build()
    val roleRequest: AssumeRoleRequest = AssumeRoleRequest.builder()
      .roleArn(properties.getStsRoleArn())
      .roleSessionName(properties.sts.roleSessionName)
      .durationSeconds(properties.sts.tokenRefreshDurationSec)
      .build()
    return StsAssumeRoleCredentialsProvider
      .builder()
      .stsClient(stsClient)
      .refreshRequest(roleRequest)
      .asyncCredentialUpdateEnabled(true)
      .build()
  }

  @Bean
  @ConditionalOnMissingBean(AthenaClient::class)
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun athenaClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): AthenaClient = AthenaClient.builder()
    .region(properties.getRegion())
    .credentialsProvider(stsAssumeRoleCredentialsProvider)
    .build()

  @Bean
  @ConditionalOnProperty("dpr.lib.aws.dynamodb.enabled", havingValue = "true")
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  @ConditionalOnMissingBean(DynamoDbClient::class)
  fun dynamoDbClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): DynamoDbClient = DynamoDbClient.builder()
    .region(properties.getRegion())
    .credentialsProvider(stsAssumeRoleCredentialsProvider)
    .build()

  @Bean
  @ConditionalOnMissingBean(RedshiftDataClient::class)
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun redshiftDataClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): RedshiftDataClient = RedshiftDataClient.builder()
    .region(properties.getRegion())
    .credentialsProvider(stsAssumeRoleCredentialsProvider)
    .build()
}
