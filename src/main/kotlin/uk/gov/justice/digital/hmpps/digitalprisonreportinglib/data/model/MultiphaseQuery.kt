package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class MultiphaseQuery(
  val index: Int,
  val datasource: String,
  val query: String,
  val parameters: List<Parameter>? = null,
)
