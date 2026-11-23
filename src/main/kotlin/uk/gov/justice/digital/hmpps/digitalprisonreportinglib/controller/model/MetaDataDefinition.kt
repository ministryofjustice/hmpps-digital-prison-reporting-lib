package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class MetaDataDefinition(
  val author: String,
  val owner: String,
  val datasource: String? = null,
)
