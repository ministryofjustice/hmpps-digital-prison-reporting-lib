package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata

data class QueryExecution(
  val rootExecutionId: String,
  val currentExecutionId: String? = null,
  val datasource: String,
  val catalog: String? = null,
  val database: String? = null,
  val index: Int,
  val query: String,
  val currentState: String? = null,
  val error: String? = null,
)
