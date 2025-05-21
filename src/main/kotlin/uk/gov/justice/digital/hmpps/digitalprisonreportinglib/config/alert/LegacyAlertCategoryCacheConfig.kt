package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.alert

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategory

@Configuration
class LegacyAlertCategoryCacheConfig {

  @Bean
  fun alertCategoryCache(): Cache<String, List<AlertCategory>> = CacheBuilder.newBuilder()
    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
    .build()
}
