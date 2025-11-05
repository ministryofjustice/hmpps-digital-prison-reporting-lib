package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.hmpps.kotlin.auth.HmppsResourceServerConfiguration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration("dprResourceServerConfiguration")
@ConditionalOnProperty(name = ["dpr.lib.user.role", "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"])
@Deprecated("Use DprSystemAuthResourceConfiguration instead")
@AutoConfigureBefore(WebMvcAutoConfiguration::class)
class DprResourceServerConfiguration(
  private val caseloadProvider: CaseloadProvider,
  @Value("\${dpr.lib.user.role}") private val authorisedRole: String,
) {

  @Order(1)
  @Bean
  fun dprSecurityFilterChain(
    http: HttpSecurity,
    dprResourceServerCustomizer: ResourceServerConfigurationCustomizer,
  ): SecurityFilterChain = HmppsResourceServerConfiguration().hmppsSecurityFilterChain(http, dprResourceServerCustomizer)

  @Bean
  fun dprResourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    oauth2 { tokenConverter = DefaultDprAuthAwareTokenConverter(caseloadProvider) }
    securityMatcher { paths = listOf("/report/**", "/reports/**", "/definitions/**", "/statements/**", "/async/**", "/missingRequest/**", "/productCollections/**") }
    anyRequestRole { defaultRole = authorisedRole.removePrefix("ROLE_") }
  }
}
