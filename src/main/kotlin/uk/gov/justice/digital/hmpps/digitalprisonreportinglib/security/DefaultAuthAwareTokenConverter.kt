package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

class DefaultAuthAwareTokenConverter(private val caseloadProvider: CaseloadProvider) : AuthAwareTokenConverter {
  private val jwtGrantedAuthoritiesConverter: Converter<Jwt, Collection<GrantedAuthority>> =
    JwtGrantedAuthoritiesConverter()

  override fun convert(jwt: Jwt): AuthAwareAuthenticationToken {
    val claims = jwt.claims
    val principal = findPrincipal(claims)
    val authorities = extractAuthorities(jwt)

    return AuthAwareAuthenticationToken(jwt, principal, authorities, caseloadProvider)
  }

  private fun findPrincipal(claims: Map<String, Any?>): String {
    return if (claims.containsKey(CLAIM_USERNAME)) {
      claims[CLAIM_USERNAME] as String
    } else if (claims.containsKey(CLAIM_USER_ID)) {
      claims[CLAIM_USER_ID] as String
    } else {
      claims[CLAIM_CLIENT_ID] as String
    }
  }

  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
    val authorities = mutableListOf<GrantedAuthority>().apply { addAll(jwtGrantedAuthoritiesConverter.convert(jwt)!!) }
    if (jwt.claims.containsKey(CLAIM_AUTHORITY)) {
      val claimAuthorities: List<String>
      if (jwt.claims[CLAIM_AUTHORITY] is String) {
        claimAuthorities = (jwt.claims[CLAIM_AUTHORITY] as String).split(",").toList()
      } else {
        @Suppress("UNCHECKED_CAST")
        claimAuthorities = (jwt.claims[CLAIM_AUTHORITY] as Collection<String>).toList()
      }

      authorities.addAll(claimAuthorities.map(::SimpleGrantedAuthority))
    }
    return authorities.toSet()
  }

  companion object {
    const val CLAIM_USERNAME = "user_name"
    const val CLAIM_USER_ID = "user_id"
    const val CLAIM_CLIENT_ID = "client_id"
    const val CLAIM_AUTHORITY = "authorities"
  }
}
