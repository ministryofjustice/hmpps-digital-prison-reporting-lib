package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext

data class Rule(val effect: Effect, val condition: List<Condition>) {
  fun execute(executionContext: ExecutionContext, transformFun: (String) -> String): Effect? =
    if (areAllConditionsPermitted(executionContext, transformFun)) {
      effect
    } else {
      null
    }

  private fun areAllConditionsPermitted(
    executionContext: ExecutionContext,
    transformFun: (String) -> String,
  ) = condition.all { it.execute(executionContext, transformFun) }
}
