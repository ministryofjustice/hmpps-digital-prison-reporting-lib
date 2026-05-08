package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOAD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOADS

class PolicyEngine(
  val policy: List<Policy>,
) {

  object VariableNames {
    const val ROLE = "\${role}"
    const val TOKEN = "\${token}"
    const val CASELOAD = "\${caseload}"
    const val CASELOADS = "\${caseloads}"
  }

  fun execute(policyType: PolicyType): String = doExecute(policy.filter { it.type == policyType }.sortedByDescending { it.type })

  fun execute(): String = doExecute(policy.sortedByDescending { it.type })

  private fun doExecute(policiesToCheck: List<Policy>): String = if (policiesToCheck.isEmpty() || isAnyPolicyDenied(policiesToCheck)) {
    POLICY_DENY
  } else {
    policiesToCheck.joinToString(" AND ") { it.apply(this::interpolateVariables) }
  }

  private fun isAnyPolicyDenied(policies: List<Policy>) = policies.map { it.execute(::interpolateVariables) }.any { it == POLICY_DENY }

  private fun interpolateVariables(s: String): String {
    if (!ExecutionContext.hasValidAuth()) return POLICY_DENY

    return when {
      s.contains(CASELOAD) -> {
        val activeCaseLoad = ExecutionContext.getActiveCaseLoadId()
        if (activeCaseLoad.isNullOrEmpty()) POLICY_DENY else s.replace(CASELOAD, activeCaseLoad)
      }
      s.contains(CASELOADS) -> {
        val caseLoads = ExecutionContext.getCaseLoadIds()
        if (caseLoads.isEmpty()) POLICY_DENY else s.replace(CASELOADS, caseLoads.joinToString { "\'${it}\'" })
      }
      else -> s
    }
  }
}
