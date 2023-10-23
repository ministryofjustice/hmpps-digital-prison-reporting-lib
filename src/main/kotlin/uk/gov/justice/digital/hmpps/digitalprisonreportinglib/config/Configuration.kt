package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class Configuration {

  @Value("\${caseloads.host}")
  lateinit var host: String

  @Value("\${caseloads.path}")
  lateinit var path: String

  @Bean
  fun webClient(): WebClient? {
    return WebClient.builder().baseUrl("$host/$path").build()
  }
}
