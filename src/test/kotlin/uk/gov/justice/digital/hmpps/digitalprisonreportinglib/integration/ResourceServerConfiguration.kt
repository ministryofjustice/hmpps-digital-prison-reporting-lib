package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.CaseloadService

@Configuration
class ResourceServerConfiguration {

  @Value("\${spring.security.user.roles}")
  private lateinit var authorisedRole: String

  @Bean
  @Throws(Exception::class)
  fun filterChain(http: HttpSecurity, caseloadService: CaseloadService): SecurityFilterChain? {
    http {
      headers { frameOptions { sameOrigin = true } }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      // Can't have CSRF protection as requires session
      csrf { disable() }
      authorizeHttpRequests {
        authorize(anyRequest, hasAuthority(authorisedRole))
      }
      oauth2ResourceServer {
        jwt {
          jwtAuthenticationConverter = AuthAwareTokenConverter(caseloadService)
        }
      }
    }
    return http.build()
  }
}
