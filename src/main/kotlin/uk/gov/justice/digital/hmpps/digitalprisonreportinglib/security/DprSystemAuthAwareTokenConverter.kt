package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareTokenConverter
import uk.gov.justice.hmpps.kotlin.auth.extractAuthorities

class DprSystemAuthAwareTokenConverter(private val userPermissionProvider: UserPermissionProvider) : AuthAwareTokenConverter() {

  override fun convert(jwt: Jwt): AuthAwareAuthenticationToken = super.convert(jwt)
    .let { authAwareAuthenticationToken ->
      DprSystemAuthAwareAuthenticationToken(
        jwt,
        authAwareAuthenticationToken.clientId,
        authAwareAuthenticationToken.userName,
        authAwareAuthenticationToken.authSource,
        extractAuthorities(jwt, JwtGrantedAuthoritiesConverter()),
        userPermissionProvider,
      )
    }
}
