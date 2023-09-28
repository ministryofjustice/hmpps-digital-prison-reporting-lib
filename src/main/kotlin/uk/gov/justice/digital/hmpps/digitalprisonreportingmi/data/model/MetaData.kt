package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model

data class MetaData(
  val author: String,
  val version: String,
  val owner: String,
  val purpose: String? = null,
  val profile: String? = null,
  val dqri: String? = null,
)
