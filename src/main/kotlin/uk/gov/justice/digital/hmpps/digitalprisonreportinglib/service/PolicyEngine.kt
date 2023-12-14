package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.PolicyEngine.VariableNames.CASELOAD

class PolicyEngine(
  val policy: List<Policy>,
  val authToken: AuthAwareAuthenticationToken? = null,
) {

  object VariableNames {
    const val ROLE = "\${role}"
    const val TOKEN = "\${token}"
    const val CASELOAD = "\${caseload}"
  }

  fun execute(): String {
    return if (isAnyPolicyDenied(policy)) {
      POLICY_DENY
    } else {
      policy.joinToString(" AND ") { it.apply(this::interpolateVariables) }
    }
  }

  private fun isAnyPolicyDenied(policies: List<Policy>) =
    policies.map { it.execute(authToken, ::interpolateVariables) }.any { it == POLICY_DENY }

  private fun interpolateVariables(s: String): String {
    var interpolated = s
    if (s.contains(CASELOAD)) {
      if (authToken?.getCaseLoads() == null) {
        return POLICY_DENY
      }
      // Note: This is currently for a single active caseload
      // Addition of single quotes could be in DPD instead
      interpolated = s.replace(CASELOAD, "'${authToken.getCaseLoads().first()}'")
    }
    return interpolated
  }
}
