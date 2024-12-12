package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

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
class AwsConfig(val properties: AwsProperties) {

  @Bean
  @ConditionalOnProperty("dpr.lib.aws.sts.enabled", havingValue = "true")
  fun stsAssumeRoleCredentialsProvider(): StsAssumeRoleCredentialsProvider {
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
  ): DynamoDbClient =
    DynamoDbClient.builder()
      .region(properties.getRegion())
      .credentialsProvider(stsAssumeRoleCredentialsProvider)
      .build()

  @Bean
  @ConditionalOnMissingBean(RedshiftDataClient::class)
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun redshiftDataClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
  ): RedshiftDataClient = RedshiftDataClient.builder()
    .region(properties.getRegion())
    .credentialsProvider(stsAssumeRoleCredentialsProvider)
    .build()
}
