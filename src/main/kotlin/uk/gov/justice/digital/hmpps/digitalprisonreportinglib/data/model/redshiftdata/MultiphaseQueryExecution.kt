package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata

data class MultiphaseQueryExecution(
  val index: Int,
  val currentState: String? = null,
  val error: String? = null,
)
