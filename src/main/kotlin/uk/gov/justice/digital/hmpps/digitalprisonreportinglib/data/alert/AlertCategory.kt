package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert

data class AlertCategory(
  val domainCode: String,
  val code: String,
  val description: String
) {
  companion object {
    const val ALL_ALERT = "All"
  }
}