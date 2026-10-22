package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.DataProductReportableInformation
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprSystemAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

fun HttpServletRequest.getUserContext(
  manageUsersClient: ManageUsersClient,
  hasProbationDatasources: Boolean,
  dataProductReportableInformation: DataProductReportableInformation,
): ExecutionContext {
  val authToken = SecurityContextHolder.getContext().authentication?.let {
    it as? DprSystemAuthAwareAuthenticationToken
      ?: throw IllegalStateException("Security context authentication was not of the type DprSystemAuthAwareAuthenticationToken but was ${it::class.qualifiedName}")
  }

  return authToken?.userName
    ?.takeUnless { it.isBlank() }
    ?.let {
      ExecutionContext(
        getCaseLoads(manageUsersClient, it, authToken.authSource),
        manageUsersClient.getUsersRoles(it),
        manageUsersClient.getUserInfo(it),
        hasProbationDatasources,
        dataProductReportableInformation,
      )
    } ?: throw UserAuthorisationException("userName on auth token was blank or did not exist")
}

fun getCaseLoads(manageUsersClient: ManageUsersClient,
                 userName: String,
                 authSource: AuthSource) : CaseloadResponse?{
  return if (authSource == AuthSource.NOMIS) {
    manageUsersClient.getCaseloads(userName)
  } else null
}
