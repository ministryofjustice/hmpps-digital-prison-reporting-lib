package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest

@Configuration
class StsCredentialsProviderConfig {

  @Bean
  @ConditionalOnProperty("dpr.lib.aws.sts.enabled", havingValue = "true")
  fun stsAssumeRoleCredentialsProvider(properties: AwsProperties): StsAssumeRoleCredentialsProvider {
    val stsClient: StsClient = StsClient.builder()
      .region(properties.typedRegion)
      .build()
    val roleRequest: AssumeRoleRequest = AssumeRoleRequest.builder()
      .roleArn(properties.stsRoleArn)
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
}
