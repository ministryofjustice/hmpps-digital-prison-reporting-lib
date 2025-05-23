package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnProperty(name = ["dpr.lib.caseloads.host"])
@Deprecated("Use UserPermissionProviderAutoConfig instead")
class CaseloadProviderAutoConfig(
  @Value("\${dpr.lib.caseloads.host}") private val caseloadHost: String,
  @Value("\${dpr.lib.caseloads.path:users/me/caseloads}") private val caseloadPath: String,
) {

  @Bean
  @Qualifier("caseloadWebClient")
  fun caseloadWebClient(): WebClient = WebClient.builder().baseUrl("$caseloadHost/$caseloadPath").build()

  @Bean
  @ConditionalOnMissingBean(CaseloadProvider::class)
  fun caseloadProvider(@Qualifier("caseloadWebClient") webClient: WebClient): CaseloadProvider = DefaultCaseloadProvider(webClient)
}
