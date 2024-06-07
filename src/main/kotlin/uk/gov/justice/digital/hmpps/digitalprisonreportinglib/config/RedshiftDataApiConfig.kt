package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.RedshiftDataRequest

@Configuration
class RedshiftDataApiConfig(
  @Value("\${dpr.lib.redshiftdataapi.database:#{null}}") val redshiftDataApiDb: String? = null,
  @Value("\${dpr.lib.redshiftdataapi.clusterid:#{null}}") val redshiftDataApiClusterId: String? = null,
  @Value("\${dpr.lib.redshiftdataapi.secretarn:#{null}}") val redshiftDataApiSecretArn: String? = null,
) {

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

  @Bean
  @ConditionalOnMissingBean(RedshiftDataRequest.Builder::class)
  fun executeStatementRequestBuilder(): ExecuteStatementRequest.Builder {
    return ExecuteStatementRequest.builder()
      .clusterIdentifier(redshiftDataApiClusterId)
      .database(redshiftDataApiDb)
      .secretArn(redshiftDataApiSecretArn)
  }
}
