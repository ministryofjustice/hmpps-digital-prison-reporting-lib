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
        .isEqualTo("Validation failure: Unsupported schemaversion: 1.1.0: Only 1.0.0 is supported at this time")
    }
  }
}
