package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizerDsl

@Configuration("dprResourceServerConfiguration")
@ConditionalOnProperty(name = ["dpr.lib.user.roles", "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"])
@AutoConfigureBefore(WebMvcAutoConfiguration::class)
class DprResourceServerConfiguration(
  private val caseloadProvider: CaseloadProvider,
  @Value("\${dpr.lib.user.roles}") private val authorisedRoles: List<String>,
) {

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    authorizeHttpRequests(removeRolePrefix(authorisedRoles))
    oauth2 { tokenConverter = DefaultDprAuthAwareTokenConverter(caseloadProvider) }
  }
}

@Configuration("dprResourceServerConfiguration")
@Deprecated("Use `dpr.lib.user.roles` instead")
@ConditionalOnProperty(name = ["dpr.lib.user.role", "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"])
@AutoConfigureBefore(WebMvcAutoConfiguration::class)
class DprResourceServerConfigurationDeprecated(
  private val caseloadProvider: CaseloadProvider,
  @Value("\${dpr.lib.user.role}") @Deprecated("Use `dpr.lib.user.roles` instead") private val authorisedRole: String,
) {

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    authorizeHttpRequests(removeRolePrefix(listOf(authorisedRole)))
    oauth2 { tokenConverter = DefaultDprAuthAwareTokenConverter(caseloadProvider) }
  }
}

fun removeRolePrefix(listOfRoles: List<String>) = listOfRoles.map { it.replace("ROLE_", "") }

private fun ResourceServerConfigurationCustomizerDsl.authorizeHttpRequests(roles: List<String>) {
  authorizeHttpRequests {
    authorize("/report/**", hasAnyRole(*roles.toTypedArray()))
    authorize("/reports/**", hasAnyRole(*roles.toTypedArray()))
    authorize("/definitions/**", hasAnyRole(*roles.toTypedArray()))
    authorize("/statements/**", hasAnyRole(*roles.toTypedArray()))
    authorize("/async/**", hasAnyRole(*roles.toTypedArray()))
  }
}
