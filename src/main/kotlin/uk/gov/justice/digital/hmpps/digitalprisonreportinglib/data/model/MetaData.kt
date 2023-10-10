package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class MetaData(
  val author: String,
  val version: String,
  val owner: String,
  val purpose: String? = null,
  val profile: String? = null,
  val dqri: String? = null,
)
