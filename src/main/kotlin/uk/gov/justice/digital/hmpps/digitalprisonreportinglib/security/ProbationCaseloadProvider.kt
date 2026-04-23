package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import jakarta.validation.ValidationException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyValueWithType

class ProbationCaseloadProvider(
  private val probationCaseloadsWebClient: WebClient,
) {
  fun getCrnAccess(username: String, crns: List<String>): ProbationAccessResponse {
    if (crns.isEmpty()) {
      throw ValidationException("Must provide a list of CRNs to validate against")
    }
    return probationCaseloadsWebClient.post()
      .uri("/users/$username/access")
      .header("Content-Type", "application/json")
      .bodyValueWithType(crns)
      .retrieve()
      .bodyToMono(ProbationAccessResponse::class.java)
      .block()!!
  }
}

data class ProbationAccessEntry(
  val crn: String,
  val userExcluded: Boolean,
  val userRestricted: Boolean,
  val exclusionMessage: String,
  val restrictionMessage: String,
)
data class ProbationAccessResponse(val access: List<ProbationAccessEntry>)
