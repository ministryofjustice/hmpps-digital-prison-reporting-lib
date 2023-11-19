package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Dataset(
  val id: String,
  val name: String,
  val query: String,
  val schema: Schema,
)
