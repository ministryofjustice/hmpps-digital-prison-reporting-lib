package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareTokenConverter
import uk.gov.justice.hmpps.kotlin.auth.extractAuthorities

class DprSystemAuthAwareTokenConverter(
  private val requiredAuthSources: List<String>,
) : AuthAwareTokenConverter() {

  override fun convert(jwt: Jwt): AuthAwareAuthenticationToken {
    val token = super.convert(jwt)
      .let { authAwareAuthenticationToken ->
        DprSystemAuthAwareAuthenticationToken(
          jwt,
          authAwareAuthenticationToken.clientId,
          authAwareAuthenticationToken.userName,
          authAwareAuthenticationToken.authSource,
          extractAuthorities(jwt, JwtGrantedAuthoritiesConverter()),
        )
      }

    if (!requiredAuthSources.map { it.lowercase() }.contains(token.authSource.name.lowercase())) {
      throw InsufficientAuthenticationException("You are not authorised to view this application.")
    }

    return token
  }
}
