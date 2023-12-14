package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Condition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthAwareAuthenticationToken

// What if I have a list of policies
class PolicyEngine(
  val policy: List<Policy>,
  val authToken: AuthAwareAuthenticationToken? = null,
) {

  private val role = "\${role}"
  private val token = "\${token}"
  private val caseload = "\${caseload}"

  val varMappings = mapOf(
    token to authToken,
    role to authToken?.authorities?.map { it.authority },
    caseload to authToken?.getCaseLoads()?.firstOrNull(),
  )

  fun execute(condition: Condition): Boolean {
    condition.match?.let { matchList ->
      if (matchList.contains(role)) {
        val userRoles = authToken?.authorities?.map { it.authority }
        return userRoles?.any { it in matchList } ?: false
      } else {
        matchList.map {
          return interpolateVariables(it).toSet().count() == 1
        }
      }
    }
    condition.exists?.map {
      return varMappings[it] != null
    }
    return false
  }

  fun execute(rule: Rule): Effect? {
    val aggregateConditionsResult = rule.condition.all { execute(it) }
    return if (aggregateConditionsResult) {
      rule.effect
    } else {
      // what if it is null
      null
    }
  }

  private fun apply(policy: Policy): String {
    if (policy.action.isEmpty()) {
      return "TRUE"
    } else {
      return policy.action.joinToString(" AND ", transform = this::interpolateVariables)
    }
  }

  private fun deny(): String {
    return "FALSE"
  }

  fun execute(): String {
    return if (anyPolicyIsDenied(policy)) {
      deny()
    } else {
      policy.joinToString(" AND ", transform = this::apply)
    }
  }

  private fun anyPolicyIsDenied(policies: List<Policy>) =
    policies.map { execute(it) }.any { it == "FALSE" }

  private fun execute(policy: Policy): String {
    var effect = Effect.PERMIT
    for (rule in policy.rule) {
      if (execute(rule) != Effect.PERMIT) {
        effect = Effect.DENY
      }
      break
    }
    return if (effect == Effect.PERMIT) {
      apply(policy)
    } else {
      deny()
    }
  }

  private fun interpolateVariables(s: String): String {
    var interpolated = s
    if (s.contains(caseload)) {
      if (varMappings[caseload] == null) {
        return deny()
      }
      // Note: This is currently for a single active caseload
      // Addition of single quotes could be in DPD instead
      interpolated = s.replace(caseload, "'${authToken?.getCaseLoads()!!.first()}'")
    }
    return interpolated
  }
}
