package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest

class ReportDefinitionIntegrationSystemTokenTest : IntegrationSystemTestBase() {

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinitionWithRolePolicy.json" }
    }
  }

  @BeforeEach
  fun setUp() {
    manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__RO"))
    manageUsersMockServer.stubLookupUserCaseload()
  }

  @Test
  fun `Definitions can be obtained with a user in context and authorised is true`() {
    webTestClient.get()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].authorised").isEqualTo("true")
  }

  @Test
  fun `Definitions can be obtained with a user in context but incorrect role and authorised is false`() {
    manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__OTHER"))
    webTestClient.get()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].authorised").isEqualTo("false")
  }

  @Test
  fun `Definitions can be obtained without a user in context but authorised is false`() {
    webTestClient.get()
      .uri("/definitions")
      .headers(setAuthorisation(user = null, roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].authorised").isEqualTo("false")
  }

  @Test
  fun `Definition details is forbidden without a user in context`() {
    webTestClient.get()
      .uri("/definitions/external-movements/last-month")
      .headers(setAuthorisation(user = null, roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Single definition is returned in expected format`() {
    try {
      prisonerRepository.save(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
      externalMovementRepository.save(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)

      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions/external-movements/last-month")
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
            "id": "external-movements",
            "name": "External Movements",
            "description": "Reports about prisoner external movements",
            "variant": {
              "id": "last-month",
              "name": "Last month",
              "interactive": true,
              "resourceName": "reports/external-movements/last-month",
              "description": "All movements in the past month",
              "specification": {
                "template": "list-section",
                "sections": [ "direction" ],
                "fields": [
                  {
                    "name": "prisonNumber",
                    "display": "Prison Number",
                    "wordWrap": null,
                    "sortable": true,
                    "defaultsort": false,
                    "type": "string",
                    "mandatory": false,
                    "visible": true,
                    "filter": {
                      "type": "Radio",
                      "staticOptions": [
                        {
                          "name": "DD105GF",
                          "display": "LastName6, F"
                        },
                        {
                          "name": "G2504UV",
                          "display": "LastName1, F"
                        },
                        {
                          "name": "G2927UV",
                          "display": "LastName1, F"
                        },
                        {
                          "name": "G3154UG",
                          "display": "LastName5, F"
                        },
                        {
                          "name": "G3411VR",
                          "display": "LastName5, F"
                        },
                        {
                          "name": "G3418VR",
                          "display": "LastName3, F"
                        }
                      ],
                      "dynamicOptions": {
                        "minimumLength": 2
                      },
                      "defaultValue": null,
                      "min": null,
                      "max": null
                    }
                  },
                  {
                    "name": "name",
                    "display": "Name",
                    "wordWrap": "none",
                    "filter": {
                      "type": "autocomplete",
                      "staticOptions": null,
                      "dynamicOptions": {
                        "minimumLength": 2
                      },
                      "defaultValue": null,
                      "min": null,
                      "max": null
                    },
                    "sortable": true,
                    "defaultsort": false,
                    "type": "string",
                    "mandatory": false,
                    "visible": true
                  },
                  {
                    "name": "date",
                    "display": "Date",
                    "wordWrap": null,
                    "filter": {
                      "type": "daterange",
                      "staticOptions": null,
                      "dynamicOptions": null,
                      "mandatory": false,
                      "min": null,
                      "max": null
                    },
                    "sortable": true,
                    "defaultsort": true,
                    "type": "date",
                    "mandatory": false,
                    "visible": true
                  },
                  {
                    "name": "origin",
                    "display": "From",
                    "wordWrap": "none",
                    "filter": {
                      "type": "text"
                    },
                    "sortable": true,
                    "defaultsort": false,
                    "type": "string",
                    "mandatory": false,
                    "visible": true
                  },
                  {
                    "name": "destination",
                    "display": "To",
                    "wordWrap": "none",
                    "filter": null,
                    "sortable": true,
                    "defaultsort": false,
                    "type": "string",
                    "visible": true,
                    "mandatory": false
                  },
                  {
                    "name": "direction",
                    "display": "Direction",
                    "wordWrap": "break-words",
                    "filter": {
                      "type": "Radio",
                      "staticOptions": [
                        {
                          "name": "in",
                          "display": "In"
                        },
                        {
                          "name": "out",
                          "display": "Out"
                        }
                      ],
                      "dynamicOptions": null,
                      "defaultValue": null,
                      "min": null,
                      "max": null
                    },
                    "sortable": true,
                    "defaultsort": false,
                    "type": "string",
                    "mandatory": false,
                    "visible": true
                  },
                  {
                    "name": "type",
                    "display": "Type",
                    "wordWrap": "normal",
                    "filter": null,
                    "sortable": true,
                    "defaultsort": false,
                    "type": "string",
                    "mandatory": false,
                    "visible": false
                  },
                  {
                    "name": "reason",
                    "display": "Reason",
                    "wordWrap": null,
                    "filter": {
                      "type": "autocomplete",
                      "dynamicOptions": {
                        "minimumLength": 2
                      },
                      "defaultValue": null,
                      "min": null,
                      "max": null
                    },
                    "sortable": true,
                    "defaultsort": false,
                    "type": "string",
                    "visible": true,
                    "mandatory": true
                  },
                  {
                    "name": "is_closed",
                    "display": "Closed",
                    "wordWrap":null,
                    "sortable": true,
                    "defaultsort":false,
                    "filter": {
                      "type": "Radio",
                      "staticOptions": [
                        {
                          "name": "false",
                          "display": "Only open"
                        },
                        {
                          "name": "true",
                          "display": "Only closed"
                        }
                      ],
                      "dynamicOptions": null,
                      "defaultValue":"false",
                      "min": null,
                      "max": null
                    },
                    "type": "boolean",
                    "mandatory": false,
                    "visible": true,
                    "calculated": false
                  },
                   {
                     "name":"origin_code",
                     "display":"Origin Code",
                     "wordWrap":null,
                     "filter":{
                        "type":"multiselect",
                        "mandatory":false,
                        "pattern":null,
                        "staticOptions":[
                          {
                              "name":"AKI",
                              "display":"Acklington (HMP)"
                           },
                           {
                              "name":"LWSTMC",
                              "display":"Lowestoft (North East Suffolk) Magistrat"
                           },
                           {
                              "name":"WWI",
                              "display":"WANDSWORTH (HMP)"
                           }
                        ],
                        "dynamicOptions":null,
                        "defaultValue":"AKI,LWSTMC,WWI",
                        "min":null,
                        "max":null,
                        "interactive":false,
                        "defaultGranularity":null,
                        "defaultQuickFilterValue":null
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
              },
              "classification": "report classification",
              "printable": true
            }
          }

          """.trimIndent(),
        )
    } finally {
      externalMovementRepository.delete(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.delete(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
    }
  }

  @Test
  fun `Single definition with an empty section list is returned in expected format`() {
    try {
      prisonerRepository.save(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
      externalMovementRepository.save(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)

      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions/external-movements/last-week")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("variant.specification.sections").isArray()
        .jsonPath("variant.specification.sections").isEmpty()
    } finally {
      externalMovementRepository.delete(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.delete(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
    }
  }
}
