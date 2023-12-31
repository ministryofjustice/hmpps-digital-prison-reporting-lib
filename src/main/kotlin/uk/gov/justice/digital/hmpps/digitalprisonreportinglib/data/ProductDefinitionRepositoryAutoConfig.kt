package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProductDefinitionRepositoryAutoConfig(
  @Value("\${dpr.lib.definition.locations}") private val definitionResourceLocations: List<String>,
) {

  @Bean
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  fun productDefinitionRepository(
    localDateTimeTypeAdaptor: LocalDateTimeTypeAdaptor,
    dprDefinitionGson: Gson,
  ): ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    definitionResourceLocations,
    dprDefinitionGson,
  )

  @Bean
  @ConditionalOnMissingBean(LocalDateTimeTypeAdaptor::class)
  fun localDateTimeTypeAdaptor(): LocalDateTimeTypeAdaptor {
    return IsoLocalDateTimeTypeAdaptor()
  }
}
