package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

data class Rule(val effect: Effect, val condition: List<Condition>) {
//  fun execute(): Effect? {
//    val aggregateConditionsResult = condition.all { it.execute() }
//    return if (aggregateConditionsResult) {
//      effect
//    } else {
//      //what if it is null
//      null
//    }
//  }
}
