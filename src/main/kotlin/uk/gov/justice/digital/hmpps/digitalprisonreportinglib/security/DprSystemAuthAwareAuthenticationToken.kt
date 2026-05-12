package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class DprSystemAuthAwareAuthenticationToken(
  jwt: Jwt,
  clientId: String,
  userName: String? = null,
  authSource: AuthSource = AuthSource.NONE,
  authorities: Collection<GrantedAuthority> = emptyList(),
) : AuthAwareAuthenticationToken(jwt, clientId, userName, authSource, authorities)