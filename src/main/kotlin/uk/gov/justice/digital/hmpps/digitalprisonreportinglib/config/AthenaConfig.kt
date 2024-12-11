package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider

@Configuration
class AthenaConfig {

  @Bean
  @ConditionalOnMissingBean(AthenaClient::class)
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun athenaClientSts(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): AthenaClient = AthenaClient.builder()
    .region(properties.typedRegion)
    .credentialsProvider(stsAssumeRoleCredentialsProvider)
    .build()

  @Bean
  @ConditionalOnMissingBean(value = [AthenaClient::class, StsAssumeRoleCredentialsProvider::class])
  fun athenaClient(
    properties: AwsProperties,
  ): AthenaClient = AthenaClient.builder()
    .region(properties.typedRegion)
    .build()
}
