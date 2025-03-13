package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.hmpps.kotlin.auth.HmppsResourceServerConfiguration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration("dprResourceServerConfiguration")
@ConditionalOnExpression(
  "!T(org.springframework.util.StringUtils).isEmpty('\$dpr.lib.system.role:}') " +
    "&& !T(org.springframework.util.StringUtils).isEmpty('\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}') " +
    "&& ('\${dpr.lib.system.authentication.enabled:false}' == 'true')",
)
@AutoConfigureBefore(WebMvcAutoConfiguration::class)
class DprSystemAuthResourceConfiguration(
  private val userPermissionProvider: UserPermissionProvider,
  @Value("\${dpr.lib.system.role}") private val systemRole: String,
) {

  @Order(1)
  @Bean
  fun dprSecurityFilterChain(
    http: HttpSecurity,
    dprResourceServerCustomizer: ResourceServerConfigurationCustomizer,
  ): SecurityFilterChain = HmppsResourceServerConfiguration().hmppsSecurityFilterChain(http, dprResourceServerCustomizer)

  @Bean
  fun dprResourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    oauth2 { tokenConverter = DprSystemAuthAwareTokenConverter(userPermissionProvider) }
    securityMatcher { paths = listOf("/report/**", "/reports/**", "/definitions/**", "/statements/**", "/async/**") }
    anyRequestRole { defaultRole = removeRolePrefix(systemRole) }
  }
}
