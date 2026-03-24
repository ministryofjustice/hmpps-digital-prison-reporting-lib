package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.module.SimpleModule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DateDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DateSerializer
import java.util.Date

@Configuration
class JacksonConfig {
  @Bean
  fun customDateModule(): SimpleModule = SimpleModule().apply {
    addSerializer(Date::class.java, DateSerializer())
    addDeserializer(Date::class.java, DateDeserializer())
  }
}
