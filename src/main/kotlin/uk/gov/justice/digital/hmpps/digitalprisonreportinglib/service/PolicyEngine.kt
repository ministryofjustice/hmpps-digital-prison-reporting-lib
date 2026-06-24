package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOAD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOADS

class PolicyEngine(
  val policy: List<Policy>,
  val executionContext: ExecutionContext,
) {

  object VariableNames {
    const val ROLE = "\${role}"
    const val TOKEN = "\${token}"
    const val CASELOAD = "\${caseload}"
    const val CASELOADS = "\${caseloads}"
  }

  fun execute(policyType: PolicyType): String = doExecute(policy.filter { it.type == policyType }.sortedByDescending { it.type })

  fun execute(): String = doExecute(policy.sortedByDescending { it.type })

  fun checkLaoPolicyExists(policies: List<Policy>): Boolean {
    return !executionContext.hasProbationDatasources || policies.any { it.type == PolicyType.LAO }
  }

  private fun doExecute(policiesToCheck: List<Policy>): String = if (policiesToCheck.isEmpty() || isAnyPolicyDenied(policiesToCheck) || !checkLaoPolicyExists(policiesToCheck)) {
    POLICY_DENY
  } else {
    policiesToCheck.joinToString(" AND ") { it.apply(this::interpolateVariables, executionContext) }
  }

  private fun isAnyPolicyDenied(policies: List<Policy>) = policies.map { it.execute(executionContext, ::interpolateVariables) }.any { it == POLICY_DENY }

  private fun interpolateVariables(s: String): String {
    if (!executionContext.hasValidAuth()) return POLICY_DENY

    return when {
      s.contains(CASELOAD) -> {
        val activeCaseLoad = executionContext.getActiveCaseLoadId()
        if (activeCaseLoad.isNullOrEmpty()) POLICY_DENY else s.replace(CASELOAD, activeCaseLoad)
      }
      s.contains(CASELOADS) -> {
        val caseLoads = executionContext.getCaseLoadIds()
        if (caseLoads.isEmpty()) POLICY_DENY else s.replace(CASELOADS, caseLoads.joinToString { "\'${it}\'" })
      }
      else -> s
    }
  }
}
