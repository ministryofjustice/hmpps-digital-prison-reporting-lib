package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import com.google.gson.annotations.SerializedName
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.InvalidDpdException

data class Policy(val id: String, val type: PolicyType, @SerializedName("action") private val _action: List<String>? = null, val rule: List<Rule>) {

  val action
    get() = _action ?: emptyList()

  object PolicyResult {
    const val POLICY_PERMIT = "TRUE"
    const val POLICY_DENY = "FALSE"
  }

  fun execute(executionContext: ExecutionContext, transformFun: (String) -> String): String {
    var effect = Effect.PERMIT
    for (r in rule) {
      if (r.execute(executionContext, transformFun) != Effect.PERMIT) {
        effect = Effect.DENY
      }
      break
    }

    if (type == PolicyType.LAO) {
      if (rule.isEmpty()) {
        throw InvalidDpdException("LAO policy without rule")
      }
      if (rule.first().effect != Effect.PERMIT && (_action == null || _action.size != 1)) {
        throw InvalidDpdException("LAO policy provided without accompanying CRN column")
      }
      effect = Effect.PERMIT
    }

    return if (effect == Effect.PERMIT) {
      apply(transformFun, executionContext)
    } else {
      POLICY_DENY
    }
  }
  fun apply(transformFunction: (String) -> String, executionContext: ExecutionContext): String {
    if (action.isEmpty() || (type == PolicyType.LAO && rule.first().effect == Effect.PERMIT)) {
      return POLICY_PERMIT
    } else {
      if (type == PolicyType.LAO) {
        return """
          ${_action!!.first()} NOT IN (
              WITH exclusions_cte as (
                SELECT crn FROM product_.lao_exclusions e WHERE e.user_id = '${executionContext.userInfo.username}' AND e.since <= NOW() AND e.until >= NOW()
              ),
              restrictions_cte as (
                SELECT crn FROM product_.lao_restrictions r 
                WHERE r.since <= NOW() AND r.until >= NOW()
                GROUP BY crn
                HAVING NOT BOOL_OR(user_id = '${executionContext.userInfo.username}')
              )
              SELECT crn FROM exclusions_cte UNION SELECT crn FROM restrictions_cte
          )
        """.trimIndent()
      }
      return action.joinToString(" AND ", transform = transformFunction)
    }
  }
}
