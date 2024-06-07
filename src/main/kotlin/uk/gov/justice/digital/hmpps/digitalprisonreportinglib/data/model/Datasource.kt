package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Datasource(
  val id: String,
  val name: String,
  val database: String? = null,
  val catalog: String? = null,
)
