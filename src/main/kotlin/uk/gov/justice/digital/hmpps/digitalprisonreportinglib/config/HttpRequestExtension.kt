package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprSystemAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient

fun HttpServletRequest.getUserContext(
  manageUsersClient: ManageUsersClient,
): ExecutionContext {
  val authToken = SecurityContextHolder.getContext().authentication?.let {
    it as? DprSystemAuthAwareAuthenticationToken
      ?: throw IllegalStateException("Security context authentication was not of the type DprSystemAuthAwareAuthenticationToken but was ${it::class.qualifiedName}")
  }

  return authToken?.userName
    ?.takeUnless { it.isBlank() }
    ?.let {
      ExecutionContext(
        manageUsersClient.getCaseloads(it),
        manageUsersClient.getUsersRoles(it),
        manageUsersClient.getUserInfo(it),
      )
    } ?: throw UserAuthorisationException("userName on auth token was blank or did not exist")
}
