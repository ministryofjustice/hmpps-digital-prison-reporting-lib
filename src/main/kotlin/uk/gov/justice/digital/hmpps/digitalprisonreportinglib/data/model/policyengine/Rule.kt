package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

data class Rule(val effect: Effect, val condition: List<Condition>) {
  fun execute(token: DprAuthAwareAuthenticationToken?, transformFun: (String) -> String): Effect? = if (areAllConditionsPermitted(token, transformFun)) {
    effect
  } else {
    null
  }

  private fun areAllConditionsPermitted(
    token: DprAuthAwareAuthenticationToken?,
    transformFun: (String) -> String,
  ) = condition.all { it.execute(token, transformFun) }
}
