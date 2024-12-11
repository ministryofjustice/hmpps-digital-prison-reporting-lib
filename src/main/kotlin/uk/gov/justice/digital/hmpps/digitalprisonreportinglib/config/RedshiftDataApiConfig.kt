package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider

@Configuration
class RedshiftDataApiConfig {

  @Bean
  @ConditionalOnMissingBean(RedshiftDataClient::class)
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun redshiftDataClientSts(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): RedshiftDataClient = RedshiftDataClient.builder()
    .region(properties.typedRegion)
    .credentialsProvider(stsAssumeRoleCredentialsProvider)
    .build()

  @Bean
  @ConditionalOnMissingBean(value = [RedshiftDataClient::class, StsAssumeRoleCredentialsProvider::class])
  fun redshiftDataClient(
    properties: AwsProperties,
  ): RedshiftDataClient = RedshiftDataClient.builder()
    .region(properties.typedRegion)
    .build()
}
