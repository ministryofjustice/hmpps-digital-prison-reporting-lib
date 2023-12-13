package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

data class Condition(
  val match: List<String>? = null,
  val exists: List<String>? = null,
) {
//  fun getType(): ConditionType {
//    match?.let { return ConditionType.MATCH }
//    exists?.let { return ConditionType.EXISTS }
//
//  }
}
