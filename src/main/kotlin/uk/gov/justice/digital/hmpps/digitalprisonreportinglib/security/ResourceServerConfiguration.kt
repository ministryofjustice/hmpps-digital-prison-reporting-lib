package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration("dprResourceServerConfiguration")
@ConditionalOnProperty(name = ["dpr.lib.user.role", "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"])
@AutoConfigureBefore(WebMvcAutoConfiguration::class)
class ResourceServerConfiguration(
  private val caseloadProvider: CaseloadProvider,
  @Value("\${dpr.lib.user.role}") private val authorisedRole: String,
) {

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    anyRequestRole { authorisedRole }
    oauth2 { tokenConverter = DefaultDprAuthAwareTokenConverter(caseloadProvider) }
  }
}
