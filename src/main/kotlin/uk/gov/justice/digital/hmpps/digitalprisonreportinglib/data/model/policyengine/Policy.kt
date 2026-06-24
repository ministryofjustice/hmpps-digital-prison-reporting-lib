package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import com.google.gson.annotations.SerializedName
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT

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
      if (rule.size != 1) {
        throw IllegalStateException("LAO policy without rule")
      }
      if (rule.first().effect != Effect.PERMIT && (_action == null || _action.size != 1)) {
        throw IllegalStateException("LAO policy provided without accompanying CRN column")
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
    if (action.isEmpty()) {
      return POLICY_PERMIT
    } else {
      if (type == PolicyType.LAO) {
        return """
          exclusions_cte as (
            select crn from product_.lao_exclusions e where e.user_id = ${executionContext.userInfo.username} AND e.since >= NOW() AND e.until <= NOW()  
          ),
          restrictions_cte as (
            select crn from restrictions r where r.user_id != ${executionContext.userInfo.username} AND r.since >= NOW() AND r.until <= NOW()
          ),  
          disallowed_crns as (select crn from exclusions_cte union restrictions_cte)
              
          ${_action!!.first()} not in (select * from disallowed_crns)
        """.trimIndent()
      }
      return action.joinToString(" AND ", transform = transformFunction)
    }
  }
}
