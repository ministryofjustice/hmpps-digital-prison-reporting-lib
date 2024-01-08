package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class ProductDefinitionRepositoryAutoConfig(
  @Value("\${dpr.lib.definition.locations:#{null}}") private val definitionResourceLocations: List<String>?,
  @Value("\${dpr.lib.dataProductDefinitions.host:#{null}}") private val definitionsHost: String?,
) {

  @Bean
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  @ConditionalOnProperty(prefix = "dpr.lib.definition", name = ["locations"])
  fun productDefinitionRepository(
    localDateTimeTypeAdaptor: LocalDateTimeTypeAdaptor,
    dprDefinitionGson: Gson,
  ): ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    definitionResourceLocations ?: emptyList(),
    dprDefinitionGson,
  )

  @Bean
  @ConditionalOnProperty(prefix = "dpr.lib.definition", name = ["locations"], matchIfMissing = true)
  fun dataProductDefinitionsRepository(
    @Qualifier("definitionsWebClient") definitionsWebClient: RestTemplate,
  ): ProductDefinitionRepository = ClientDataProductDefinitionsRepository(
    definitionsWebClient,
    definitionsHost,
  )

  @Bean
  @ConditionalOnMissingBean(LocalDateTimeTypeAdaptor::class)
  fun localDateTimeTypeAdaptor(): LocalDateTimeTypeAdaptor {
    return IsoLocalDateTimeTypeAdaptor()
  }
}
