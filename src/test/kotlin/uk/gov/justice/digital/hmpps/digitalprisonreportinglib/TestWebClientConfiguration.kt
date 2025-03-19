package uk.gov.justice.digital.hmpps.digitalprisonreportinglib

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@TestConfiguration
class TestWebClientConfiguration(
  @Value("\${hmpps.manage-users.url}")
  private val manageUsersApiUri: String,
  @Value("\${api.timeout:20s}")
  private val healthTimeout: Duration,
) {

  @Bean
  fun manageUsersWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "DPR_LIB_API",
    url = manageUsersApiUri,
    healthTimeout,
  )
}
