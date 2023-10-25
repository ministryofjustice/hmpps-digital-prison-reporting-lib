package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SecurityAutoConfig(
  @Value("\${dpr.lib.caseloads.host}") private val caseloadHost: String,
  @Value("\${dpr.lib.caseloads.path:me/caseloads}") private val caseloadPath: String,
) {

  @Bean
  @Qualifier("caseloadWebClient")
  fun caseloadWebClient(): WebClient {
    return WebClient.builder().baseUrl("$caseloadHost/$caseloadPath").build()
  }

  @Bean
  @ConditionalOnMissingBean(AuthAwareTokenConverter::class)
  fun authAwareTokenConverter(
    caseloadProvider: CaseloadProvider,
  ): AuthAwareTokenConverter {
    return DefaultAuthAwareTokenConverter(caseloadProvider)
  }

  @Bean
  @ConditionalOnMissingBean(CaseloadProvider::class)
  fun caseloadProvider(@Qualifier("caseloadWebClient") webClient: WebClient): CaseloadProvider {
    return DefaultCaseloadProvider(webClient)
  }
}
