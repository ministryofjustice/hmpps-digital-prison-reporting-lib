package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOAD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOADS
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.ROLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.TOKEN

data class Condition(
  val match: List<String>? = null,
  val exists: List<String>? = null,
) {

  fun execute(authToken: DprAuthAwareAuthenticationToken?, interpolateVariables: (String) -> String): Boolean {
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
    authToken: DprAuthAwareAuthenticationToken?,
    interpolateVariables: (String) -> String,
  ): Boolean = if (matchList.contains(ROLE)) {
    isAnyOfTheRolesInTheList(authToken, matchList)
  } else {
    isTheInterpolatedVarInTheList(matchList, interpolateVariables)
  }

  private fun isAnyOfTheRolesInTheList(
    authToken: DprAuthAwareAuthenticationToken?,
    matchList: List<String>,
  ): Boolean {
    val userRoles = authToken?.getRoles()
    return userRoles?.any { it in matchList } ?: false
  }

  private fun isTheInterpolatedVarInTheList(
    matchList: List<String>,
    interpolateVariables: (String) -> String,
  ) = matchList.map {
    interpolateVariables(it)
  }.toSet().count() == 1

  private fun isNotNull(
    authToken: DprAuthAwareAuthenticationToken?,
    varPlaceholder: String,
  ): Boolean {
    val varMappings = mapOf(
      TOKEN to authToken,
      ROLE to authToken?.getRoles(),
      CASELOAD to authToken?.getActiveCaseLoadId(),
      CASELOADS to authToken?.getCaseLoadIds(),
    )
    return varMappings[varPlaceholder] != null
  }
}
