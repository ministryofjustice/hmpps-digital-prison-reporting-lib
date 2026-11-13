package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.InvalidDpdException

@Serializable
data class Policy(
  val id: String,
  val type: PolicyType,
  @SerializedName("action")
  @SerialName("action")
  private val _action: List<String>? = null,
  val rule: List<Rule>,
  val name: String? = null,
  val description: String? = null,
) {

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
        return POLICY_PERMIT
      }
      return action.joinToString(" AND ", transform = transformFunction)
    }
  }
}
