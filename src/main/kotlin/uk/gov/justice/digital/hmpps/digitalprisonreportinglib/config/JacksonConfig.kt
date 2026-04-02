package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.module.SimpleModule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DateSqlDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DateSqlSerializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DateUtilDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DateUtilSerializer
import java.util.Date

@Configuration
class JacksonConfig {
  @Bean
  fun customDateModule(): SimpleModule = SimpleModule().apply {
    // java.util.Date date format
    addSerializer(Date::class.java, DateUtilSerializer())
    addDeserializer(Date::class.java, DateUtilDeserializer())

    // java.sql.Date date format
    addSerializer(java.sql.Date::class.java, DateSqlSerializer())
    addDeserializer(java.sql.Date::class.java, DateSqlDeserializer())
  }
}
