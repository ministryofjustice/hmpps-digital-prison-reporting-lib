package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt

interface CaseloadProvider {

  fun getActiveCaseloadId(jwt: Jwt): String

  fun getCaseloadIds(jwt: Jwt): List<String>
}
