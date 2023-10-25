package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt

interface CaseloadProvider {

  fun getActiveCaseloadIds(jwt: Jwt): List<String>
}
