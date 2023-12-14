package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthAwareAuthenticationToken

data class Rule(val effect: Effect, val condition: List<Condition>) {
  fun execute(token: AuthAwareAuthenticationToken?, transformFun: (String) -> String): Effect? {
    return if (areAllConditionsPermitted(token, transformFun)) {
      effect
    } else {
      null
    }
  }

  private fun areAllConditionsPermitted(
    token: AuthAwareAuthenticationToken?,
    transformFun: (String) -> String,
  ) = condition.all { it.execute(token, transformFun) }
}
