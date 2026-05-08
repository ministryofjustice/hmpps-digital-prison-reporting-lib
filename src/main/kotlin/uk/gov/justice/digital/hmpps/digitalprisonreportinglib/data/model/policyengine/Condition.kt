package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOAD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOADS
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.ROLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.TOKEN

data class Condition(
  val match: List<String>? = null,
  val exists: List<String>? = null,
) {

  fun execute(interpolateVariables: (String) -> String): Boolean {
    match?.let { matchList ->
      return executeMatch(matchList, interpolateVariables)
    }
    exists?.map {
      return isNotNull(it)
    }
    return false
  }

  private fun executeMatch(
    matchList: List<String>,
    interpolateVariables: (String) -> String,
  ): Boolean = if (matchList.contains(ROLE)) {
    isAnyOfTheRolesInTheList(matchList)
  } else {
    isTheInterpolatedVarInTheList(matchList, interpolateVariables)
  }

  private fun isAnyOfTheRolesInTheList(
    matchList: List<String>,
  ): Boolean {
    val userRoles = ExecutionContext.get().userRoles.removeRolePrefix()
    return userRoles.any { it in matchList.removeRolePrefix() }
  }

  private fun isTheInterpolatedVarInTheList(
    matchList: List<String>,
    interpolateVariables: (String) -> String,
  ) = matchList.map {
    interpolateVariables(it)
  }.toSet().count() == 1

  private fun isNotNull(
    varPlaceholder: String,
  ): Boolean {
    val varMappings = mapOf(
      TOKEN to ExecutionContext.hasValidAuth(),
      ROLE to ExecutionContext.get().userRoles,
      CASELOAD to ExecutionContext.getActiveCaseLoadId(),
      CASELOADS to ExecutionContext.getCaseLoadIds(),
    )
    return varMappings[varPlaceholder] != null
  }
}

fun List<String>.removeRolePrefix(): List<String> = this.map { it.removePrefix("ROLE_") }
