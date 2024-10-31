package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

interface AuthenticationHelper {
  fun getCurrentAuthentication(): DprAuthAwareAuthenticationToken
}
