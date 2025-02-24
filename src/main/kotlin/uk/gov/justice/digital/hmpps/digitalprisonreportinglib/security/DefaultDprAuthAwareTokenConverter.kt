package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareTokenConverter
import uk.gov.justice.hmpps.kotlin.auth.extractAuthorities

class DefaultDprAuthAwareTokenConverter(private val caseloadProvider: CaseloadProvider) : AuthAwareTokenConverter() {

  override fun convert(jwt: Jwt): DprAuthAwareAuthenticationToken = super.convert(jwt)
    .let { authAwareAuthenticationToken ->
      DprAuthAwareAuthenticationToken(
        jwt,
        authAwareAuthenticationToken.clientId,
        authAwareAuthenticationToken.userName,
        authAwareAuthenticationToken.authSource,
        extractAuthorities(jwt, JwtGrantedAuthoritiesConverter()),
        caseloadProvider,
      )
    }
}
