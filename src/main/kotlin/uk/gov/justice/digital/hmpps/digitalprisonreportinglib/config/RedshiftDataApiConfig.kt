package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient

@Configuration
class RedshiftDataApiConfig {

  @Bean
  @ConditionalOnMissingBean(RedshiftDataClient::class)
  fun redshiftDataClient(): RedshiftDataClient {
    return RedshiftDataClient.builder()
      .region(Region.EU_WEST_2)
      .build()
  }

  @Bean
  @ConditionalOnMissingBean(AthenaClient::class)
  fun athenaClient(): AthenaClient {
    val region = Region.EU_WEST_2
    return AthenaClient.builder()
      .region(region)
      .build()
  }
}
