package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata

data class StatementExecutionStatus(
  val status: String,
  // The amount of time in nanoseconds that the statement ran.
  val duration: Long,
  val queryString: String,
  val resultRows: Long,
  // The size in bytes of the returned results. A -1 indicates the value is null.
  val resultSize: Long?,
  val error: String? = null,
)
