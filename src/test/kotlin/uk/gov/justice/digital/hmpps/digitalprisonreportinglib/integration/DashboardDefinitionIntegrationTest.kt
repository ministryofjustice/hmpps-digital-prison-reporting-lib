package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriBuilder

class DashboardDefinitionIntegrationTest : IntegrationTestBase() {
  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinitionWithDashboard.json" }
    }
  }

  @Test
  fun `Dashboard definition is returned as expected`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions/missing-ethnicity-metrics/dashboards/age-breakdown-dashboard-1")
          .build()
      }
      .headers(setAuthorisation(roles = authorisedRoles))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        """
            {
            "id": "age-breakdown-dashboard-1",
            "name": "Age Breakdown Dashboard",
            "description": "Age Breakdown Dashboard Description",
            "sections": [{
              "id": "totals-breakdown",
              "display": "Totals breakdown",
              "visualisations": [ {
                    "id": "total-prisoners",
                    "type": "list",
                    "display": "Total prisoners by wing",
                    "columns": {
                      "keys": [
                        {
                          "id": "establishment_id",
                          "display": "Establishmnent ID"
                        },
                        {
                          "id": "wing",
                          "display": "Wing"
                        }
                      ],
                      "measures": [
                        {
                          "id": "establishment_id",
                          "display": "Establishmnent ID"
                        },
                        {
                          "id": "wing",
                          "display": "Wing"
                        },
                        {
                          "id": "total_prisoners",
                          "display": "Total prisoners"
                        }
                      ],
                      "expectNulls": true
                    }
            }]
             }],              
            "filterFields": [
              {
                "name":"establishment_id",
                "display":"Establishment ID",
                "filter": {
                  "type":"Select",
                  "mandatory":false,
                  "staticOptions": [
                    {
                      "name":"AAA",
                      "display":"Aardvark"
                    },
                    {
                      "name":"BBB",
                      "display":"Bumblebee"
                    }
                  ],
                  "dynamicOptions": {
                    "minimumLength":null
                  },
                  "interactive":true
                },
                "sortable":true,
                "defaultsort":false,
                "type":"string",
                "mandatory":false,
                "visible":true,
                "calculated":false,
                "header":false
              }
            ] 
          }
        """.trimIndent(),
      )
  }
}
