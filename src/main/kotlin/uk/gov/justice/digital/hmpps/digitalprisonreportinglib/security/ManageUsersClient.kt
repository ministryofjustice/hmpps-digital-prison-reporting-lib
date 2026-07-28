package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.TimeoutException
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import java.io.IOException
import java.time.Duration

const val WARNING_NO_ACTIVE_CASELOAD = "User has not set an active caseload."
const val WARNING_NO_CASELOADS = "User does not have any caseloads."

class ManageUsersClient(
  private val manageUsersWebClient: WebClient,
  private val requiredAuthSources: List<String>,
) {
  /**
   * @throws NoDataAvailableException if caseloads is empty and user is NOMIS user with [WARNING_NO_CASELOADS]
   * @throws NoDataAvailableException if no active caseload and user is NOMIS user with [WARNING_NO_ACTIVE_CASELOAD]
   */
  fun getCaseloads(username: String): CaseloadResponse {
    val caseloadResponse = fetchCaseloadInfo(username)

    if (caseloadResponse.caseloads.isEmpty() || caseloadResponse.activeCaseload == null) {
      val userInfo = getUserInfo(username)
      if (userInfo.authSource != AuthSource.NOMIS) {
        return CaseloadResponse(
          username = caseloadResponse.username,
          active = true,
          accountType = caseloadResponse.accountType,
          caseloads = caseloadResponse.caseloads,
          activeCaseload = caseloadResponse.activeCaseload,
        )
      }
      if (caseloadResponse.caseloads.isEmpty()) {
        throw NoDataAvailableException(WARNING_NO_CASELOADS)
      }

      throw NoDataAvailableException(WARNING_NO_ACTIVE_CASELOAD)
    }

    return caseloadResponse
  }

  fun getUserInfo(username: String): AuthUser {
    val userInfo = manageUsersWebClient.get()
      .uri("/users/$username")
      .header("Content-Type", "application/json")
      .retrieve()
      .bodyToMono(AuthUser::class.java)
      .retryWhen(retryWithExponentialBackOffAndJitter)
      .block()!!
    if (!requiredAuthSources.map { it.lowercase() }.contains(userInfo.authSource.name.lowercase())) {
      throw UserAuthorisationException("User attempted to access service with the wrong auth source")
    }
    return userInfo
  }

  fun getUsersRoles(username: String): List<String> = manageUsersWebClient.get()
    .uri("/users/$username/roles")
    .header("Content-Type", "application/json")
    .retrieve()
    .bodyToMono(ROLES)
    .retryWhen(retryWithExponentialBackOffAndJitter)
    .block()!!.map { it.roleCode }

  fun fetchCaseloadInfo(username: String): CaseloadResponse = manageUsersWebClient.get()
    .uri("/prisonusers/$username/caseloads")
    .header("Content-Type", "application/json")
    .exchangeToMono { response ->
      if (response.statusCode().value() == 404) {
        response.releaseBody().thenReturn(
          CaseloadResponse(
            username = username,
            active = false,
            accountType = "GENERAL",
            caseloads = emptyList(),
            activeCaseload = null,
          ),
        )
      } else if (response.statusCode().is5xxServerError) {
        response.createException().flatMap { Mono.error(it) }
      } else {
        response
          .bodyToMono(CaseloadResponse::class.java)
          .map { caseload ->
            caseload.copy(caseloads = caseload.caseloads.sortedBy { it.id })
          }
      }
    }
    .retryWhen(retryWithExponentialBackOffAndJitter)
    .block()!!

  private val retryWithExponentialBackOffAndJitter = Retry
    .backoff(3, Duration.ofMillis(500))
    .maxBackoff(Duration.ofSeconds(5))
    .jitter(0.5)
    .filter { throwable ->
      when (throwable) {
        is WebClientResponseException ->
          throwable.statusCode.is5xxServerError

        is WebClientRequestException,
        is ConnectTimeoutException,
        is ReadTimeoutException,
        is TimeoutException,
        is IOException,
        -> true

        else -> false
      }
    }
}
data class RolesResponse(val roleCode: String)

private val ROLES: ParameterizedTypeReference<List<RolesResponse>> =
  object : ParameterizedTypeReference<List<RolesResponse>>() {}

data class CaseloadResponse(val username: String, val active: Boolean, val accountType: String, val activeCaseload: Caseload?, val caseloads: List<Caseload>)
