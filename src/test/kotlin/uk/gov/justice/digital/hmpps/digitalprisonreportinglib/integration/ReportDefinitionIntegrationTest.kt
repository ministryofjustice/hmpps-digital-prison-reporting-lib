package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest
import java.time.LocalDate.now
import java.time.format.DateTimeFormatter

class ReportDefinitionIntegrationTest : IntegrationTestBase() {

  class ReportDefinitionListTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinition.json, productDefinition2.json" }
      }
    }

    @Test
    fun `Definition list is returned as expected`() {
      val result = webTestClient.get()
        .uri("/definitions")
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ReportDefinition>()
        .returnResult()

      assertThat(result.responseBody).isNotNull
      assertThat(result.responseBody).hasSize(2)
      assertThat(result.responseBody).first().isNotNull
      val reportDefinition2 = result.responseBody!![1]
      assertThat(reportDefinition2).isNotNull

      val definition = result.responseBody!!.first()

      assertThat(definition.name).isEqualTo("External Movements")
      assertThat(reportDefinition2.name).isEqualTo("External Movements Duplicate")
      assertThat(definition.description).isEqualTo("Reports about prisoner external movements")
      assertThat(definition.variants).hasSize(2)
      assertThat(definition.variants[0]).isNotNull
      assertThat(definition.variants[1]).isNotNull

      val lastMonthVariant = definition.variants[0]

      assertThat(lastMonthVariant.id).isEqualTo("last-month")
      assertThat(lastMonthVariant.resourceName).isEqualTo("reports/external-movements/last-month")
      assertThat(lastMonthVariant.name).isEqualTo("Last month")
      assertThat(lastMonthVariant.description).isEqualTo("All movements in the past month")
      assertThat(lastMonthVariant.specification).isNotNull
      assertThat(lastMonthVariant.specification?.fields).hasSize(8)

      val lastWeekVariant = definition.variants[1]
      assertThat(lastWeekVariant.id).isEqualTo("last-week")
      assertThat(lastWeekVariant.resourceName).isEqualTo("reports/external-movements/last-week")
      assertThat(lastWeekVariant.description).isEqualTo("All movements in the past week")
      assertThat(lastWeekVariant.name).isEqualTo("Last week")
      assertThat(lastWeekVariant.specification).isNotNull
      assertThat(lastWeekVariant.specification?.fields).hasSize(8)

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/me/caseloads")).size).isEqualTo(1)
    }
  }

  @Test
  fun `Definitions are returned when they match the filter`() {
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
      .expectBodyList<ReportDefinition>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(1)
    assertThat(result.responseBody).first().isNotNull

    val definition = result.responseBody!!.first()

    assertThat(definition.variants).hasSize(2)
  }

  @Test
  fun `Definitions are not returned when they do not match the filter`() {
    val result = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions")
          .queryParam("renderMethod", "SVG")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinition>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(0)
  }

  @Test
  fun `Definitions do not contain null values`() {
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
    assertThat(result.responseBody).doesNotContain(": null")
  }

  @Test
  fun `Single definition is returned as expected limiting the static options results to maxStaticOptions`() {
    try {
      prisonerRepository.save(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
      externalMovementRepository.save(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)

      val result = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions/external-movements/last-month")
            .queryParam("maxStaticOptions", "1")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<SingleVariantReportDefinition>()
        .returnResult()

      assertThat(result.responseBody).isNotNull

      val definition = result.responseBody!!

      assertThat(definition.name).isEqualTo("External Movements")
      assertThat(definition.description).isEqualTo("Reports about prisoner external movements")
      assertThat(definition.variant).isNotNull

      val lastMonthVariant = definition.variant

      assertThat(lastMonthVariant.id).isEqualTo("last-month")
      assertThat(lastMonthVariant.resourceName).isEqualTo("reports/external-movements/last-month")
      assertThat(lastMonthVariant.name).isEqualTo("Last month")
      assertThat(lastMonthVariant.description).isEqualTo("All movements in the past month")
      assertThat(lastMonthVariant.specification).isNotNull
      assertThat(lastMonthVariant.specification?.fields).hasSize(8)

      val directionField = lastMonthVariant.specification?.fields?.find { it.name == "direction" }

      assertThat(directionField).isNotNull
      assertThat(directionField!!.filter).isNotNull
      assertThat(directionField.filter!!.type).isEqualTo(FilterType.Radio)
      assertThat(directionField.filter!!.staticOptions).isNotNull
      assertThat(directionField.filter!!.staticOptions).hasSize(2)
      assertThat(directionField.filter!!.staticOptions!![0].name).isEqualTo("in")
      assertThat(directionField.filter!!.staticOptions!![0].display).isEqualTo("In")
      assertThat(directionField.filter!!.staticOptions!![1].name).isEqualTo("out")
      assertThat(directionField.filter!!.staticOptions!![1].display).isEqualTo("Out")

      val dateField = lastMonthVariant.specification?.fields?.find { it.name == "date" }

      assertThat(dateField).isNotNull
      assertThat(dateField!!.filter).isNotNull
      assertThat(dateField.filter!!.type).isEqualTo(FilterType.DateRange)
      val lastMonth = now().minusMonths(1).format(DateTimeFormatter.ISO_DATE)
      val thisMonth = now().format(DateTimeFormatter.ISO_DATE)
      assertThat(dateField.filter!!.defaultValue).isEqualTo("$lastMonth - $thisMonth")

      val reasonField = lastMonthVariant.specification?.fields?.find { it.name == "reason" }
      assertThat(reasonField).isNotNull
      assertThat(reasonField!!.filter).isNotNull
      assertThat(reasonField.filter!!.type).isEqualTo(FilterType.AutoComplete)
      assertThat(reasonField.filter!!.staticOptions).isNotNull
      assertThat(reasonField.filter!!.staticOptions).hasSize(1)
      assertThat(reasonField.filter!!.staticOptions!![0].name).isEqualTo("Transfer In from Other Establishment")
      assertThat(reasonField.filter!!.staticOptions!![0].display).isEqualTo("Transfer In from Other Establishment")
    } finally {
      externalMovementRepository.delete(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.delete(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
    }
  }

  @Test
  fun `the json response from the definitions endpoint is returned with the expected format`() {
    try {
      prisonerRepository.save(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
      externalMovementRepository.save(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)

      webTestClient.get()
        .uri("/definitions")
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          """
          [
      {
          "description": "Reports about prisoner external movements",
          "id": "external-movements",
          "name": "External Movements",
          "variants": [
              {
                  "description": "All movements in the past month",
                  "id": "last-month",
                  "name": "Last month",
                  "resourceName": "reports/external-movements/last-month",
                  "specification": {
                      "fields": [
                          {
                              "defaultsort": false,
                              "display": "Prison Number",
                              "filter": null,
                              "name": "prisonNumber",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "Name",
                              "filter": {   
                                 "dynamicOptions": {
                                    "minimumLength": 2,
                                    "returnAsStaticOptions": false
                                },
                                "staticOptions": null,
                                 "type": "autocomplete"
                              },
                              "name": "name",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": "None"
                          },
                          {
                              "defaultsort": true,
                              "display": "Date",
                              "filter": {
                                  "staticOptions": null,
                                  "type": "daterange"
                              },
                              "name": "date",
                              "sortable": true,
                              "type": "date",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "From",
                              "filter": null,
                              "name": "origin",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": "None"
                          },
                          {
                              "defaultsort": false,
                              "display": "To",
                              "filter": null,
                              "name": "destination",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": "None"
                          },
                          {
                              "defaultsort": false,
                              "display": "Direction",
                              "filter": {
                                  "defaultValue": null,
                                  "staticOptions": [
                                      {
                                          "display": "In",
                                          "name": "in"
                                      },
                                      {
                                          "display": "Out",
                                          "name": "out"
                                      }
                                  ],
                                  "type": "Radio"
                              },
                              "name": "direction",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "Type",
                              "filter": null,
                              "name": "type",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "Reason",
                              "filter": {
                                  "type": "autocomplete",
                                  "dynamicOptions": {
                                      "minimumLength": 2,
                                      "returnAsStaticOptions": true
                                  },
                                  "staticOptions": [
                                  {
                                        "name": "Transfer In from Other Establishment",
                                        "display": "Transfer In from Other Establishment"
                                  },
                                  {
                                      "display": "Transfer Out to Other Establishment",
                                      "name": "Transfer Out to Other Establishment"
                                  }
                                ],
                                "defaultValue": null
                              },
                              "name": "reason",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          }
                      ],
                      "template": "list"
                  }
              },
              {
                  "description": "All movements in the past week",
                  "id": "last-week",
                  "name": "Last week",
                  "resourceName": "reports/external-movements/last-week",
                  "specification": {
                      "fields": [
                          {
                              "defaultsort": false,
                              "display": "Prison Number",
                              "filter": null,
                              "name": "prisonNumber",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "Name",
                              "filter": {   
                                 "dynamicOptions": {
                                    "minimumLength": 2,
                                    "returnAsStaticOptions": false
                                },
                                "staticOptions": null,
                                 "type": "autocomplete"
                              },
                              "name": "name",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": "None"
                          },
                          {
                              "defaultsort": true,
                              "display": "Date",
                              "filter": {
                                  "staticOptions": null,
                                  "type": "daterange"
                              },
                              "name": "date",
                              "sortable": true,
                              "type": "date",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "From",
                              "filter": null,
                              "name": "origin",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": "None"
                          },
                          {
                              "defaultsort": false,
                              "display": "To",
                              "filter": null,
                              "name": "destination",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": "None"
                          },
                          {
                              "defaultsort": false,
                              "display": "Direction",
                              "filter": {
                                  "defaultValue": null,
                                  "staticOptions": [
                                      {
                                          "display": "In",
                                          "name": "in"
                                      },
                                      {
                                          "display": "Out",
                                          "name": "out"
                                      }
                                  ],
                                  "type": "Radio"
                              },
                              "name": "direction",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "Type",
                              "filter": null,
                              "name": "type",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          },
                          {
                              "defaultsort": false,
                              "display": "Reason",
                              "filter": {
                                  "type": "autocomplete",
                                  "dynamicOptions": {
                                      "minimumLength": 2,
                                      "returnAsStaticOptions": true
                                  },
                                  "staticOptions": [
                                  {
                                        "name": "Transfer In from Other Establishment",
                                        "display": "Transfer In from Other Establishment"
                                  },
                                  {
                                      "display": "Transfer Out to Other Establishment",
                                      "name": "Transfer Out to Other Establishment"
                                  }
                                ],
                                "defaultValue": null
                              },
                              "name": "reason",
                              "sortable": true,
                              "type": "string",
                              "wordWrap": null
                          }
                      ],
                      "template": "list"
                  }
              }
          ]
      }
  ]
      """,
        )
    } finally {
      externalMovementRepository.delete(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.delete(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
    }
  }
}
