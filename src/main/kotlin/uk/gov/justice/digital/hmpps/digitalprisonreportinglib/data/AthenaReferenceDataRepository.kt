package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.Row
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

abstract class AthenaReferenceDataRepository<T>(
  override val athenaClient: AthenaClient,
  override val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  override val athenaWorkgroup: String,
) : AthenaApiRepository(athenaClient, tableIdGenerator, athenaWorkgroup) {

  companion object {
    const val NOMIS_CATALOG = "nomis"
    const val DIGITAL_PRISON_REPORTING_DB = "DIGITAL_PRISON_REPORTING"
  }

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

  abstract fun mapRow(page: Int, index: Int, row: Row): T?
}
