package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.auditing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.time.Instant
import kotlin.time.Clock
import kotlin.time.toJavaInstant

enum class AuditAction {
  DATA,
  DOWNLOAD,
}

enum class AuditStatus {
  ATTEMPT,
  SUCCESS,
  FAILURE,
}

@Service
class AuditService(
  private val hmppsAuditService: HmppsAuditService? = null,
  private val scope: CoroutineScope,
  @Value($$"${dpr.lib.serviceName}") private val serviceName: String,
) {
  fun <T> runAuditableAction(
    auditAction: AuditAction,
    executionContext: ExecutionContext,
    action: () -> T,
  ): T {
    try {
      val result = action()
      publish(auditAction, AuditStatus.SUCCESS, executionContext)
      return result
    } catch (e: Throwable) {
      publish(auditAction, AuditStatus.FAILURE, executionContext)
      throw e
    }
  }

  fun publishAttempt(action: AuditAction, executionContext: ExecutionContext) {
    when (action) {
      AuditAction.DATA -> scope.launch { auditDataRequestAttemptEvent(executionContext) }
      AuditAction.DOWNLOAD -> scope.launch { auditDataDownloadRequestAttemptEvent(executionContext) }
    }
  }
  fun publish(
    action: AuditAction,
    status: AuditStatus,
    executionContext: ExecutionContext,
  ) {
    when (action) {
      AuditAction.DATA -> {
        when (status) {
          AuditStatus.ATTEMPT -> auditDataRequestAttemptEvent(executionContext)
          AuditStatus.SUCCESS -> auditDataRequestCompletionEvent(true, executionContext)
          AuditStatus.FAILURE -> auditDataRequestCompletionEvent(false, executionContext)
        }
      }
      AuditAction.DOWNLOAD -> {
        when (status) {
          AuditStatus.ATTEMPT -> auditDataDownloadRequestAttemptEvent(executionContext)
          AuditStatus.SUCCESS -> auditDataDownloadRequestCompletionEvent(true, executionContext)
          AuditStatus.FAILURE -> auditDataDownloadRequestCompletionEvent(false, executionContext)
        }
      }
    }
  }
  fun auditDataRequestAttemptEvent(
    executionContext: ExecutionContext,
  ) {
    publishEvent(
      what = "VIEW_DATA_ATTEMPT",
      subjectId = executionContext.dataProductReportableInformation.id + "_" + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = Json.encodeToString(generateAuditDetails(executionContext)),
    )
  }
  fun auditDataRequestCompletionEvent(success: Boolean, executionContext: ExecutionContext) {
    publishEvent(
      what = "VIEW_DATA_${if (success) "SUCCESS" else "FAILURE"}",
      subjectId = executionContext.dataProductReportableInformation.id + "_" + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = "",
    )
  }
  fun auditDataDownloadRequestAttemptEvent(executionContext: ExecutionContext) {
    publishEvent(
      what = "DOWNLOAD_DATA_ATTEMPT",
      subjectId = executionContext.dataProductReportableInformation.id + "_" + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = Json.encodeToString(generateAuditDetails(executionContext)),
    )
  }
  fun auditDataDownloadRequestCompletionEvent(success: Boolean, executionContext: ExecutionContext) {
    publishEvent(
      what = "DOWNLOAD_DATA_${if (success) "SUCCESS" else "FAILURE"}",
      subjectId = executionContext.dataProductReportableInformation.id + "_" + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = Json.encodeToString(generateAuditDetails(executionContext)),
    )
  }

  private fun generateAuditDetails(executionContext: ExecutionContext): AuditDetails = AuditDetails(
    reportAndVariantName = executionContext.dataProductReportableInformation.let { if (it.name != null && it.variantName != null) it.name + "_" + it.variantName else null },
    preFilters = executionContext.preFilters,
    postFilters = executionContext.postFilters,
    sort = executionContext.sortColumnName
      ?.takeIf { executionContext.sortColumnDirection != null }
      ?.let { mapOf(it to executionContext.sortColumnDirection!!) }
      .orEmpty(),
    columns = executionContext.columns,
    policies = executionContext.policies,
  )

  private fun publishEvent(
    what: String,
    subjectId: String? = null,
    subjectType: String? = null,
    `when`: Instant,
    who: String,
    service: String? = null,
    details: String? = null,
  ) {
    if (hmppsAuditService == null) {
      return
    }
    scope.launch {
      hmppsAuditService.publishEvent(
        what = what,
        subjectId = subjectId,
        subjectType = subjectType,
        `when` = `when`,
        who = who,
        service = service,
        details = details,
      )
    }
  }
}

@Serializable
data class AuditDetails(
  private val reportAndVariantName: String? = null,
  private val preFilters: Map<String, String>,
  private val sort: Map<String, String>,
  private val postFilters: Map<String, String>,
  private val columns: List<String>,
  private val policies: List<Policy>,
  private val prefix: String? = null,
)
