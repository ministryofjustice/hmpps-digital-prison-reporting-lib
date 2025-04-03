package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DatasetQuery(
  val index: Int,
  val datasource: Datasource,
  val query: String,
  val parameters: List<Parameter>? = null,
)
