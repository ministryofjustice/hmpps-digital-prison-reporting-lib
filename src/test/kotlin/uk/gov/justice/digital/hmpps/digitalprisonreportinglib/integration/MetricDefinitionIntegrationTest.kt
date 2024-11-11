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
          .path("/definitions/missing-ethnicity-metrics/dashboards/test-dashboard-1")
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
                "id": "missing-ethnicity-metric",
                "name": "Missing Ethnicity By Establishment Metric",
                "display": "Missing Ethnicity By Establishment Metric",
                "description": "Missing Ethnicity By Establishment Metric",
                "charts": [
                  { "type": "bar", "dimension": "establishment_id" }
                ],
                "data": [
                  [
                    {
                      "name": "ethnicity_is_missing",
                      "display": "No. of Prisoners without ethnicity",
                      "unit": "number"
                    },
                    {
                      "name": "has_ethnicity",
                      "display": "No. of Prisoners with ethnicity",
                      "unit": "number"
                    }
                  ]
                ]
              }
            ]
          }
        """.trimIndent(),
      )
  }
}
