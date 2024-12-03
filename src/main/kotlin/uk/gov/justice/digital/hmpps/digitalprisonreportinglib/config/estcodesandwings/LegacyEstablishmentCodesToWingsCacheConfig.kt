package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.estcodesandwings

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentAndWing

@Configuration
class LegacyEstablishmentCodesToWingsCacheConfig {
  @Bean
  fun refreshCacheTaskScheduler() = ThreadPoolTaskScheduler().apply { initialize() }

  @Bean
  fun establishmentCodesCache(): Cache<String, List<EstablishmentAndWing>> = CacheBuilder.newBuilder()
    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
    .build()
}
