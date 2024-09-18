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
                  "id": "test-metric-id-1"
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
            "display": "Missing Ethnicity",
            "description": "Missing Ethnicity",
            "specification":
            [
              {
                "name": "establishment_id",
                "display": "Establishment ID",
                "group": true
              },
              {
                "name": "missing_ethnicity_percentage",
                "display": "% Missing Ethnicity",
                "chart": ["doughnut"],
                "unit": "percentage"
              },
              {
                "name": "present_ethnicity_percentage",
                "display": "% With Ethnicity",
                "chart": ["doughnut"],
                "unit": "percentage"
              },
              {
                "name": "no_of_prisoners",
                "display": "No. of Prisoners with ethnicity",
                "chart": ["bar"]
              },
              {
                "name": "no_of_prisoners_without",
                "display": "No. of Prisoners without ethnicity",
                "chart": ["bar"]
              },
              {
                "name": "random_data",
                "display": "Random Data"
              }
            ]
          }
        """.trimIndent(),
      )
  }
}
