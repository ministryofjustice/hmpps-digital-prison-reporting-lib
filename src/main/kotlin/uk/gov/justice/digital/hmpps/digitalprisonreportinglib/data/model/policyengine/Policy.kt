package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import com.google.gson.annotations.SerializedName
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

data class Policy(val id: String, val type: PolicyType, @SerializedName("action") private val _action: List<String>? = null, val rule: List<Rule>) {

  val action
    get() = _action ?: emptyList()

  object PolicyResult {
    const val POLICY_PERMIT = "TRUE"
    const val POLICY_DENY = "FALSE"
  }
  fun execute(userToken: DprAuthAwareAuthenticationToken?, transformFun: (String) -> String): String {
    var effect = Effect.PERMIT
    for (r in rule) {
      if (r.execute(userToken, transformFun) != Effect.PERMIT) {
        effect = Effect.DENY
      }
      break
    }
    return if (effect == Effect.PERMIT) {
      apply(transformFun)
    } else {
      POLICY_DENY
    }
  }
  fun apply(transformFunction: (String) -> String): String = if (action.isEmpty()) {
    POLICY_PERMIT
  } else {
    action.joinToString(" AND ", transform = transformFunction)
  }
}
