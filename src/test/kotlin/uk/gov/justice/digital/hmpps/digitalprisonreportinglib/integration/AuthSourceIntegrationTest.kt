package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriBuilder
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class AuthSourceIntegrationTest : IntegrationTestBase() {
  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun setupClass(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  @Test
  fun `check endpoints reject user when authsource does not match`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), authSource = AuthSource.DELIUS))
      .exchange()
      .expectStatus()
      .isForbidden
  }
}

class EmptyAuthSourceEnvVarAuthSourceIntegrationTest : IntegrationTestBase() {
  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun setupClass(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
      registry.add("dpr.lib.user.requiredAuthSources") { "" }
    }
  }

  @Test
  fun `check value populates default if empty`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), authSource = AuthSource.DELIUS))
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), authSource = AuthSource.NOMIS))
      .exchange()
      .expectStatus()
      .isOk
  }
}
