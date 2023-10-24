package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProductDefinitionRepositoryAutoConfig {

  @Bean
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  fun productDefinitionRepository (
    localDateTypeAdaptor: LocalDateTypeAdaptor
  ) : ProductDefinitionRepository  {

    return JsonFileProductDefinitionRepository(localDateTypeAdaptor)
  }

  @Bean
  @ConditionalOnMissingBean(LocalDateTypeAdaptor::class)
  fun localDateTypeAdaptor (
  ) : LocalDateTypeAdaptor  {
    return IsoLocalDateTypeAdaptor()
  }

}