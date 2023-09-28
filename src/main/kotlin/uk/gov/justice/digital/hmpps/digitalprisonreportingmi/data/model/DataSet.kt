package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model

data class DataSet(
  val id: String,
  val name: String,
  val query: String,
  val schema: Schema,
)
