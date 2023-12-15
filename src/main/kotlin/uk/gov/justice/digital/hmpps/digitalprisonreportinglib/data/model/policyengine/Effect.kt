package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

enum class Effect(val type: String) {
  PERMIT("permit"),
  DENY("deny"),
  ;
}
