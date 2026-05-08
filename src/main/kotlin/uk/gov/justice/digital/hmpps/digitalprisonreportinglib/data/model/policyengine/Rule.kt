package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

data class Rule(val effect: Effect, val condition: List<Condition>) {
  fun execute(transformFun: (String) -> String): Effect? = if (areAllConditionsPermitted(transformFun)) {
    effect
  } else {
    null
  }

  private fun areAllConditionsPermitted(
    transformFun: (String) -> String,
  ) = condition.all { it.execute(transformFun) }
}
