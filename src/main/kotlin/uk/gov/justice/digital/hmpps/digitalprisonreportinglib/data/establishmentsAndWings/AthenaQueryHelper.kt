package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_ABORTED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FAILED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FINISHED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus

class AthenaQueryHelper {

  fun waitForQueryToComplete(
    executionId: String,
    getStatementStatus: (statementId: String) -> StatementExecutionStatus,
  ) {
    var isQueryStillRunning = true
    while (isQueryStillRunning) {
      val status = getStatementStatus(executionId)
      when (status.status) {
        QUERY_FAILED -> {
          throw RuntimeException(
            "Query Failed to run with Error Message: " +
              status.stateChangeReason,
          )
        }
        QUERY_ABORTED -> {
          throw RuntimeException("Query was cancelled.")
        }
        QUERY_FINISHED -> {
          isQueryStillRunning = false
        }
        else -> {
          Thread.sleep(500)
        }
      }
    }
  }
}
