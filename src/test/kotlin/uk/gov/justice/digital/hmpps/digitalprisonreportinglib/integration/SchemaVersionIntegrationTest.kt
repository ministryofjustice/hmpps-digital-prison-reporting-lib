package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.ErrorResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary

class SchemaVersionIntegrationTest {

  class KotlinxDeserialisationV110PositiveTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(
        registry: DynamicPropertyRegistry,
      ) {
        registry.add("dpr.lib.definition.locations") {
          "strictlyValidDefinition.json"
        }
      }
    }

    @Test
    fun `Get definitions returns a 200 response for strictly valid DPD with schemaversion 1_1_0 `() {
      val result = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions")
            .queryParam("renderMethod", "HTML")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(String::class.java)
        .returnResult()

      assertThat(result.responseBody).isNotNull
    }

    @Test
    fun `Get definition summaries returns a 200 response for strictly valid DPD with schemaversion 1_1_0 `() {
      val result = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions/external-movements-strict")
            .queryParam("renderMethod", "HTML")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(String::class.java)
        .returnResult()

      assertThat(result.responseBody).isNotNull
    }

    @Test
    fun `Get single definition returns a 200 response for strictly valid DPD with schemaversion 1_1_0 `() {
      val result = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions/external-movements-strict/last-month")
            .queryParam("renderMethod", "HTML")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(String::class.java)
        .returnResult()

      assertThat(result.responseBody).isNotNull
    }
  }

  class KotlinxDeserialisationV110NegativeTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(
        registry: DynamicPropertyRegistry,
      ) {
        registry.add("dpr.lib.definition.locations") {
          "productDefinitionLenientBut1_1_0.json,strictlyValidDefinition.json"
        }
      }
    }

    @Test
    fun `Get definitions returns a 400 response for not strictly valid DPD with schemaversion 1_1_0 `() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions")
            .queryParam("renderMethod", "HTML")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `Get definition summaries returns a 400 response for not strictly valid DPD with schemaversion 1_1_0 `() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions/external-movements-strict")
            .queryParam("renderMethod", "HTML")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `Get single definition returns a 400 response for not strictly valid DPD with schemaversion 1_1_0 `() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions/external-movements-strict/last-month")
            .queryParam("renderMethod", "HTML")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isBadRequest
    }
  }

  class ValidSchemaVersionTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") {
          "productDefinitionValidVersion1_0_0.json," +
            "productDefinitionNoSchemaVersion.json"
        }
      }
    }

    @Test
    fun `Definition list is returned as expected when schema version is valid`() {
      val result = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ReportDefinitionSummary>()
        .returnResult()

      assertThat(result.responseBody).isNotNull
      assertThat(result.responseBody).hasSize(2)
      val ids = result.responseBody!!.map { it.id }
      assertThat(ids).containsOnly("external-movements")
    }
  }

  class InvalidSchemaVersionTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionInvalidVersion.json" }
      }
    }

    @Test
    fun `Returns 400 when product definition has unsupported schema version`() {
      val result = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult()

      assertThat(result.responseBody?.userMessage)
        .isEqualTo("Validation failure: Unsupported schemaversion: 1.2.0: Supported versions are 1.0.0 and 1.1.0.")
    }
  }
}
