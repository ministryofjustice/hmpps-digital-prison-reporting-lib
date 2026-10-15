package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.auditing

import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.SortDirection
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.getExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.DataProductReportableInformation
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import kotlin.time.Clock
import kotlin.time.toJavaInstant

enum class AuditAction {
  DATA_REQUEST_ATTEMPT,
  DATA_REQUEST_SUCCESS,
  DATA_REQUEST_FAILURE,
  DOWNLOAD_REQUEST_ATTEMPT,
  DOWNLOAD_REQUEST_SUCCESS,
  DOWNLOAD_REQUEST_FAILURE,
}

@Service
class AuditService(
  private val hmppsAuditService: HmppsAuditService,
  private val scope: CoroutineScope,
  private val manageUsersClient: ManageUsersClient,
  @Value($$"${dpr.lib.serviceName}") private val serviceName: String,
) {
  fun <T> runAuditableAction(
    hasProbationDatasources: Boolean,
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    auditAttemptAction: AuditAction,
    auditSuccessAction: AuditAction,
    auditFailureAction: AuditAction,
    request: HttpServletRequest,
    selectedColumns: List<String>? = emptyList(),
    sortColumn: String? = null,
    sortedAsc: Boolean? = null,
    action: (executionContext: ExecutionContext) -> T,
  ): T {
    val executionContext = request.getExecutionContext(
      manageUsersClient,
      hasProbationDatasources,
      DataProductReportableInformation(
        id = reportId,
        variantId = reportVariantId,
      ),
      preFilters = filters,
      columns = selectedColumns.orEmpty(),
      sortColumnName = sortColumn,
      sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
    )
    scope.launch {
      publish(auditAttemptAction, executionContext)
    }
    try {
      val result = action(executionContext)
      scope.launch {
        publish(auditSuccessAction, executionContext)
      }
      return result
    } catch (e: Throwable) {
      scope.launch {
        publish(auditFailureAction, executionContext)
      }
      throw e
    }
  }
  suspend fun publish(action: AuditAction, executionContext: ExecutionContext) {
    when (action) {
      AuditAction.DATA_REQUEST_ATTEMPT -> auditDataRequestAttemptEvent(executionContext)
      AuditAction.DATA_REQUEST_FAILURE -> auditDataRequestCompletionEvent(false, executionContext)
      AuditAction.DATA_REQUEST_SUCCESS -> auditDataRequestCompletionEvent(true, executionContext)
      AuditAction.DOWNLOAD_REQUEST_ATTEMPT -> auditDataDownloadRequestAttemptEvent(executionContext)
      AuditAction.DOWNLOAD_REQUEST_FAILURE -> auditDataDownloadRequestCompletionEvent(false, executionContext)
      AuditAction.DOWNLOAD_REQUEST_SUCCESS -> auditDataDownloadRequestCompletionEvent(true, executionContext)
    }
  }
  suspend fun auditDataRequestAttemptEvent(
    executionContext: ExecutionContext,
  ) {
    hmppsAuditService.publishEvent(
      what = "VIEW_DATA_ATTEMPT",
      subjectId = executionContext.dataProductReportableInformation.id + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = Json.encodeToString(generateAuditDetails(executionContext)),
    )
  }
  suspend fun auditDataRequestCompletionEvent(success: Boolean, executionContext: ExecutionContext) {
    hmppsAuditService.publishEvent(
      what = "VIEW_DATA_${if (success) "SUCCESS" else "FAILURE"}",
      subjectId = executionContext.dataProductReportableInformation.id + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = "",
    )
  }
  suspend fun auditDataDownloadRequestAttemptEvent(executionContext: ExecutionContext) {
    hmppsAuditService.publishEvent(
      what = "DOWNLOAD_DATA_ATTEMPT",
      subjectId = executionContext.dataProductReportableInformation.id + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = "",
    )
  }
  suspend fun auditDataDownloadRequestCompletionEvent(success: Boolean, executionContext: ExecutionContext) {
    hmppsAuditService.publishEvent(
      what = "DOWNLOAD_DATA_${if (success) "SUCCESS" else "FAILURE"}",
      subjectId = executionContext.dataProductReportableInformation.id + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = "",
    )
  }

  private fun generateAuditDetails(executionContext: ExecutionContext): AuditDetails = AuditDetails(
    reportAndVariantName = executionContext.dataProductReportableInformation.name + "_" + executionContext.dataProductReportableInformation.variantName,
    preFilters = executionContext.preFilters,
    postFilters = executionContext.postFilters,
    sort = executionContext.sortColumnName
      ?.takeIf { executionContext.sortColumnDirection != null }
      ?.let { mapOf(it to executionContext.sortColumnDirection!!) }
      .orEmpty(),
    columns = executionContext.columns,
    policies = executionContext.policies,
  )
}

data class AuditDetails(
  private val reportAndVariantName: String,
  private val preFilters: Map<String, String>,
  private val sort: Map<String, String>,
  private val postFilters: Map<String, String>,
  private val columns: List<String>,
  private val policies: List<Policy>,
)
