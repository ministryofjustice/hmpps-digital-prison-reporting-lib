package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component("dprContextHelper")
class ContextAuthenticationHelper : AuthenticationHelper {
  override fun getCurrentAuthentication() =
    SecurityContextHolder.getContext().authentication as DprAuthAwareAuthenticationToken
}
