package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOAD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.ROLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.TOKEN

data class Condition(
  val match: List<String>? = null,
  val exists: List<String>? = null,
) {

  fun execute(authToken: AuthAwareAuthenticationToken?, interpolateVariables: (String) -> String): Boolean {
    match?.let { matchList ->
      return executeMatch(matchList, authToken, interpolateVariables)
    }
    exists?.map {
      return isNotNull(authToken, it)
    }
    return false
  }

  private fun executeMatch(
    matchList: List<String>,
    authToken: AuthAwareAuthenticationToken?,
    interpolateVariables: (String) -> String,
  ): Boolean {
    return if (matchList.contains(ROLE)) {
      isAnyOfTheRolesInTheList(authToken, matchList)
    } else {
      isTheInterpolatedVarInTheList(matchList, interpolateVariables)
    }
  }

  private fun isAnyOfTheRolesInTheList(
    authToken: AuthAwareAuthenticationToken?,
    matchList: List<String>,
  ): Boolean {
    val userRoles = authToken?.authorities?.map { it.authority }
    return userRoles?.any { it in matchList } ?: false
  }

  private fun isTheInterpolatedVarInTheList(
    matchList: List<String>,
    interpolateVariables: (String) -> String,
  ) = matchList.map {
    interpolateVariables(it)
  }.toSet().count() == 1

  private fun isNotNull(
    authToken: AuthAwareAuthenticationToken?,
    varPlaceholder: String,
  ): Boolean {
    val varMappings = mapOf(
      TOKEN to authToken,
      ROLE to authToken?.authorities?.map { it.authority },
      CASELOAD to authToken?.getCaseLoads()?.firstOrNull(),
    )
    return varMappings[varPlaceholder] != null
  }
}
