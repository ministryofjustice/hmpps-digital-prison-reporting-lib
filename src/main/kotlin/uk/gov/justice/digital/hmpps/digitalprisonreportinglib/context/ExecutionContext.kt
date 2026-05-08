package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

data class ExecutionContext(
  val prisonCaseloadData: CaseloadResponse,
  val userRoles: List<String>,
  val userInfo: AuthUser,
) {
  companion object {
    fun get(): ExecutionContext = ExecutionContextHolder.getContext()
    fun clear() {
      ExecutionContextHolder.clearContext()
    }
    fun getActiveCaseLoadId(): String? = ExecutionContextHolder.getContext().prisonCaseloadData.activeCaseload?.id
    fun hasValidAuth(): Boolean {
      val context = ExecutionContextHolder.getContext()
      return context.prisonCaseloadData.username.isNotBlank() && context.prisonCaseloadData.active && context.userInfo.username.isNotBlank() && context.userInfo.active && context.userInfo.authSource != AuthSource.NONE
    }
    fun getCaseLoadIds(): List<String> = ExecutionContextHolder.getContext().prisonCaseloadData.caseloads.map { it.id }
  }
}

fun ExecutionContext.set() = apply { ExecutionContextHolder.setContext(this) }

@Component
class ExecutionContextHolder {
  companion object {
    private var context: ThreadLocal<ExecutionContext> =
      ThreadLocal.withInitial {
        ExecutionContext(
          CaseloadResponse(
            username = "",
            active = false,
            accountType = "GENERAL",
            activeCaseload = null,
            caseloads = emptyList(),
          ),
          emptyList(),
          AuthUser(
            username = "",
            active = false,
            name = "",
            authSource = AuthSource.NONE,
            userId = "",
            uuid = "",
          ),
        )
      }

    internal fun getContext(): ExecutionContext = context.get()
    internal fun setContext(emc: ExecutionContext) {
      context.set(emc)
    }

    internal fun clearContext() {
      context.remove()
    }
  }
}
