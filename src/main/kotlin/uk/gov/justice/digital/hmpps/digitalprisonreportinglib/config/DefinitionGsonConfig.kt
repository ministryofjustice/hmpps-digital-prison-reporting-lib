package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.FilterTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.LocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PolicyTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RuleEffectTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import java.time.LocalDateTime

@Configuration
class DefinitionGsonConfig {

  @Bean("dprDefinitionGson")
  fun definitionGson(
    localDateTimeTypeAdaptor: LocalDateTimeTypeAdaptor,
  ): Gson = GsonBuilder()
    .registerTypeAdapter(LocalDateTime::class.java, localDateTimeTypeAdaptor)
    .registerTypeAdapter(FilterType::class.java, FilterTypeDeserializer())
    .registerTypeAdapter(Effect::class.java, RuleEffectTypeDeserializer())
    .registerTypeAdapter(PolicyType::class.java, PolicyTypeDeserializer())
    .create()
}
