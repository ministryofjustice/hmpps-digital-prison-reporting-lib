package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata

import io.swagger.v3.oas.annotations.media.Schema

data class StatementExecutionStatus(
  @Schema(
    examples = ["FINISHED", "SUBMITTED", "STARTED", "PICKED", "FAILED", "ALL", "ABORTED"],
    description = "The status of the statement execution.",
  )
  val status: String,
  @Schema(
    example = "10562762848",
    description = "The amount of time in nanoseconds that the statement ran.",
  )
  val duration: Long,
  @Schema(
    example = "10",
    description = "The number of rows returned from the query.",
  )
  val resultRows: Long,
  @Schema(
    example = "0",
    description = "The size in bytes of the returned results. A -1 indicates the value is null.",
  )
  val resultSize: Long?,
  @Schema(description = "Contains a short description of the error that occurred.")
  val error: String? = null,
  @Schema(
    examples = ["1", "2", "3"],
    description = "Specific to Athena queries. An integer value that specifies the category of a query failure error. " +
      "The following list shows the category for each integer value.\n" +
      "1 - System\n" +
      "2 - User\n" +
      "3 - Other",
  )
  val errorCategory: Int? = null,
  @Schema(description = "Specific to Athena queries. Further detail about the status of the query.")
  val stateChangeReason: String? = null,
)
