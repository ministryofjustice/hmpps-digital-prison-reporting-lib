package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DynamicFilterOption(
  val minimumLength: Int? = null,
  val returnAsStaticOptions: Boolean,
  val maximumOptions: Long? = null,
  val dataset: String? = null,
  val name: String? = null,
  val display: String? = null,
)
