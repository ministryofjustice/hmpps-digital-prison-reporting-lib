package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriBuilder

class MetricDefinitionIntegrationTest : IntegrationTestBase() {
  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinitionWithMetrics.json" }
    }
  }

  @Test
  fun `All Dashboard definitions are returned as expected`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions/dashboards")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        """
          [{
             "id": "test-dashboard-1",
              "name": "Test Dashboard 1",
              "description": "Test Dashboard 1 Description",
              "metrics": [
                {
                  "id": "test-metric-id-1",
                  "visualisationType": ["bar"]
                }
              ]
          }]
        """.trimIndent(),
      )
  }

  @Test
  fun `Dashboard definition is returned as expected`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions/external-movements/dashboards/test-dashboard-1")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        """
          {
             "id": "test-dashboard-1",
              "name": "Test Dashboard 1",
              "description": "Test Dashboard 1 Description",
              "metrics": [
                {
                  "id": "test-metric-id-1",
                  "visualisationType": ["bar"]
                }
              ]
          }
        """.trimIndent(),
      )
  }

  @Test
  fun `Metric definition is returned as expected`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions/external-movements/metrics/test-metric-id-1")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        """
          {
            "id": "test-metric-id-1",
            "name": "testMetricId1",
            "display": "Prisoner Images by Status Percentage",
            "description": "Prisoner Images by Status Percentage",
            "visualisationType": [
              "bar",
              "doughnut"
            ],
            "specification":
            [
              {
                "name": "status",
                "display": "Status"
              },
              {
                "name": "count",
                "display": "Count",
                "unit": "percentage"
              }
            ]
          }
        """.trimIndent(),
      )
  }
}
