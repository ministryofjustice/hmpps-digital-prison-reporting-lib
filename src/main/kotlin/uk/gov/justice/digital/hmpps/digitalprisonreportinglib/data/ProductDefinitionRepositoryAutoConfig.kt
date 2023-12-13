package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

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
  ): ProductDefinitionRepository {
    return JsonFileProductDefinitionRepository(
      localDateTimeTypeAdaptor,
      definitionResourceLocations,
      FilterTypeDeserializer(),
      SchemaFieldTypeDeserializer(),
      RuleEffectTypeDeserializer(),
      PolicyTypeDeserializer(),
    )
  }

  @Bean
  @ConditionalOnMissingBean(LocalDateTimeTypeAdaptor::class)
  fun localDateTimeTypeAdaptor(): LocalDateTimeTypeAdaptor {
    return IsoLocalDateTimeTypeAdaptor()
  }
}
