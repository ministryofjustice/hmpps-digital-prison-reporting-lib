package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareTokenConverter
import uk.gov.justice.hmpps.kotlin.auth.extractAuthorities

@Deprecated("Use DprSystemAuthAwareTokenConverter instead")
class DefaultDprAuthAwareTokenConverter(private val caseloadProvider: CaseloadProvider) : AuthAwareTokenConverter() {

  override fun convert(jwt: Jwt): AuthAwareAuthenticationToken = super.convert(jwt)
    .let { authAwareAuthenticationToken ->
      DprUserAuthAwareAuthenticationToken(
        jwt,
        authAwareAuthenticationToken.clientId,
        authAwareAuthenticationToken.userName,
        authAwareAuthenticationToken.authSource,
        extractAuthorities(jwt, JwtGrantedAuthoritiesConverter()),
        caseloadProvider,
      )
    }
}
