package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties

interface AuthenticationResolver {
  fun resolve(jwt: OAuth2ResourceServerProperties.Jwt): Context? {
    return null
  }
}
