package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@AutoConfigureBefore(WebMvcAutoConfiguration::class)
@Configuration("dprResourceServerConfiguration")
@ConditionalOnProperty(name = ["dpr.lib.user.role"])
class ResourceServerConfiguration(
  private val caseloadProvider: CaseloadProvider,
  @Value("\${dpr.lib.user.role}") private val authorisedRole: String,
) {

  @Bean("dprFilterChain")
  fun hmppsSecurityFilterChain(
    http: HttpSecurity,
    customizer: ResourceServerConfigurationCustomizer,
  ): SecurityFilterChain = http {
    sessionManagement { SessionCreationPolicy.STATELESS }
    headers { frameOptions { sameOrigin = true } }
    csrf { disable() }
    authorizeHttpRequests {
      customizer.authorizeHttpRequestsCustomizer.dsl
        // override the entire authorizeHttpRequests DSL
        ?.also { dsl -> dsl.invoke(this) }
        // apply specific customizations to the default authorizeHttpRequests DSL
        ?: also {
          customizer.unauthorizedRequestPathsCustomizer.unauthorizedRequestPaths.forEach { authorize(it, permitAll) }
          customizer.anyRequestRoleCustomizer.defaultRole
            ?.also { authorize(anyRequest, hasRole(it)) }
            ?: also {
              authorize("/report/**", hasRole(authorisedRole))
              authorize("/reports/**", hasRole(authorisedRole))
              authorize("/definitions/**", hasRole(authorisedRole))
              authorize("/statements/**", hasRole(authorisedRole))
              authorize(anyRequest, authenticated)
            }
        }
    }
    oauth2ResourceServer {
      jwt { jwtAuthenticationConverter = DefaultDprAuthAwareTokenConverter(caseloadProvider) }
    }
  }
    .let { http.build() }
}
