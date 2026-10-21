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
  private val hmppsAuditService: HmppsAuditService,
  private val scope: CoroutineScope,
  @Value($$"${dpr.lib.serviceName}") private val serviceName: String,
  @Value($$"${dpr.lib.auditEnabled}") private val auditEnabled: Boolean,
) {
  fun <T> runAuditableAction(
    auditAction: AuditAction,
    executionContext: ExecutionContext,
    action: () -> T,
  ): T {
    if (!auditEnabled) {
      return action()
    }
    try {
      val result = action()
      scope.launch {
        publish(auditAction, AuditStatus.SUCCESS, executionContext)
      }
      return result
    } catch (e: Throwable) {
      scope.launch {
        publish(auditAction, AuditStatus.FAILURE, executionContext)
      }
      throw e
    }
  }

  fun publishAttempt(action: AuditAction, executionContext: ExecutionContext) {
    when (action) {
      AuditAction.DATA -> scope.launch { auditDataRequestAttemptEvent(executionContext) }
      AuditAction.DOWNLOAD -> scope.launch { auditDataDownloadRequestAttemptEvent(executionContext) }
    }
  }
  suspend fun publish(
    action: AuditAction,
    status: AuditStatus,
    executionContext: ExecutionContext,
  ) {
    if (!auditEnabled) {
      return
    }
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
  suspend fun auditDataRequestAttemptEvent(
    executionContext: ExecutionContext,
  ) {
    hmppsAuditService.publishEvent(
      what = "VIEW_DATA_ATTEMPT",
      subjectId = executionContext.dataProductReportableInformation.id + "_" + executionContext.dataProductReportableInformation.variantId,
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
      subjectId = executionContext.dataProductReportableInformation.id + "_" + executionContext.dataProductReportableInformation.variantId,
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
      subjectId = executionContext.dataProductReportableInformation.id + "_" + executionContext.dataProductReportableInformation.variantId,
      subjectType = "REPORT_ID",
      `when` = Clock.System.now().toJavaInstant(),
      who = executionContext.userInfo.uuid,
      service = serviceName,
      details = Json.encodeToString(generateAuditDetails(executionContext)),
    )
  }
  suspend fun auditDataDownloadRequestCompletionEvent(success: Boolean, executionContext: ExecutionContext) {
    hmppsAuditService.publishEvent(
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
