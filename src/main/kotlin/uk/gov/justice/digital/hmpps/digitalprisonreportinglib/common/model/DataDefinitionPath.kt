package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model

enum class DataDefinitionPath(val value: String) {
  ORPHANAGE("definitions/prisons/orphanage"),

  PROBATION("definitions/probation"),

  MISSING("definitions/prisons/missing"),

  OTHER(""),
}
