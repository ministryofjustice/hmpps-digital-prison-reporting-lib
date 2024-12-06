package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOAD

class PolicyEngine(
  val policy: List<Policy>,
  val authToken: DprAuthAwareAuthenticationToken? = null,
) {

  object VariableNames {
    const val ROLE = "\${role}"
    const val TOKEN = "\${token}"
    const val CASELOAD = "\${caseload}"
  }

  fun execute(policyType: PolicyType): String {
    return doExecute(policy.filter { it.type == policyType }.sortedByDescending { it.type })
  }

  fun execute(): String = doExecute(policy.sortedByDescending { it.type })

  private fun doExecute(policiesToCheck: List<Policy>): String {
    return if (policiesToCheck.isEmpty() || isAnyPolicyDenied(policiesToCheck)) {
      POLICY_DENY
    } else {
      policiesToCheck.joinToString(" AND ") { it.apply(this::interpolateVariables) }
    }
  }

  private fun isAnyPolicyDenied(policies: List<Policy>) =
    policies.map { it.execute(authToken, ::interpolateVariables) }.any { it == POLICY_DENY }

  private fun interpolateVariables(s: String): String {
    var interpolated = s
    if (s.contains(CASELOAD)) {
      if (authToken == null || authToken.getCaseLoads().isEmpty()) {
        return POLICY_DENY
      }
      // Note: This is currently for a single active caseload
      // Addition of single quotes could be in DPD instead
      interpolated = s.replace(CASELOAD, authToken.getCaseLoads().first())
    }
    return interpolated
  }
}
