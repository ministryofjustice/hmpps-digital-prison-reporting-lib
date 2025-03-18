package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder

@Primary
@Component
class TestAuthenticationHelper : HmppsAuthenticationHolder() {

  override lateinit var authentication: AuthAwareAuthenticationToken
}
