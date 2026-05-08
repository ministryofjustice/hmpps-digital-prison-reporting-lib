package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.set
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ResponseHeader
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprSystemAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient
import java.util.Collections.singletonList

@Configuration
class ExecutionContextConfig(private val contextInterceptor: ExecutionContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry
      .addInterceptor(contextInterceptor)
      .addPathPatterns("/**")
      .excludePathPatterns(
        "/health/**",
        "/info",
        "/ping",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
      )
  }
}

@Configuration
class ExecutionContextInterceptor(
  private val manageUsersClient: ManageUsersClient,
) : HandlerInterceptor {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    val authToken = SecurityContextHolder.getContext().authentication?.let {
      it as? DprSystemAuthAwareAuthenticationToken
        ?: throw IllegalStateException("Security context authentication was not of the type DprSystemAuthAwareAuthenticationToken but was ${it::class.qualifiedName}")
    }
    authToken?.userName.takeUnless { it.isNullOrBlank() }
      ?.let {
        val caseloads = try {
          manageUsersClient.getCaseloads(it)
        } catch (exception: NoDataAvailableException) {
          val headers = HttpHeaders()
          headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason).toString()

          response.setHeader(ResponseHeader.NO_DATA_WARNING_HEADER_NAME, singletonList(exception.reason).toString())

          return true
        }
        ExecutionContext(
          caseloads,
          manageUsersClient.getUsersRoles(it),
          manageUsersClient.getUserInfo(it),
        ).set()
      }
    return true
  }

  override fun afterCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    ex: Exception?,
  ) {
    ExecutionContext.clear()
    super.afterCompletion(request, response, handler, ex)
  }
}
