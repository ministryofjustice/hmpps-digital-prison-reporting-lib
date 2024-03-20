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

@AutoConfigureBefore(WebMvcAutoConfiguration::class)
@Configuration("dprResourceServerConfiguration")
@ConditionalOnProperty(name = ["dpr.lib.user.role", "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"])
class ResourceServerConfiguration(
  @Value("\${dpr.lib.user.role}") private var authorisedRole: String,
) {

  @Bean("dprFilterChain")
  @Throws(Exception::class)
  fun filterChain(http: HttpSecurity, tokenConverter: DprAuthAwareTokenConverter): SecurityFilterChain? {
    http {
      headers { frameOptions { sameOrigin = true } }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      // Can't have CSRF protection as requires session
      csrf { disable() }
      authorizeHttpRequests {
        listOf(
          "/health/**",
          "/info",
          "/error",
          "/v3/api-docs",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/swagger-resources",
          "/swagger-resources/configuration/ui",
          "/swagger-resources/configuration/security",
        ).forEach { authorize(it, permitAll) }
        authorize(anyRequest, hasAuthority(authorisedRole))
      }
      oauth2ResourceServer {
        jwt {
          jwtAuthenticationConverter = tokenConverter
        }
      }
    }
    return http.build()
  }
}
