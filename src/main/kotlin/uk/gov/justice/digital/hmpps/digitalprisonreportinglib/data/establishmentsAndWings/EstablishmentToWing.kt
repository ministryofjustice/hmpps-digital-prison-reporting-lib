package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings

data class EstablishmentToWing(val establishmentCode: String, val description: String, val wing: String) {
  companion object {
    const val ALL_WINGS = "All"
  }
}
