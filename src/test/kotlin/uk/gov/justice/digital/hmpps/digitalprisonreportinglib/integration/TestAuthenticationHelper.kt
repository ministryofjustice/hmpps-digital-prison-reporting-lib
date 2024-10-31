package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthenticationHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Primary
@Component
class TestAuthenticationHelper : AuthenticationHelper {

  lateinit var authentication: DprAuthAwareAuthenticationToken

  override fun getCurrentAuthentication() = authentication
}
