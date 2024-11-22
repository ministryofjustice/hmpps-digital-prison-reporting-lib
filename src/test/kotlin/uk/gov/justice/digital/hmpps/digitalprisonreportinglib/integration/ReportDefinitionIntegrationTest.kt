package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest
import java.time.LocalDate.now
import java.time.format.DateTimeFormatter

class ReportDefinitionIntegrationTest : IntegrationTestBase() {

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  class ReportDefinitionListTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinition.json, dpd001-court-hospital-movements.json" }
      }
    }

    @Test
    fun `Definition list is returned as expected`() {
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
      assertThat(result.responseBody).first().isNotNull
      val courtAndHospitalMovementsReport = result.responseBody!![1]
      assertThat(courtAndHospitalMovementsReport).isNotNull

      val definition = result.responseBody!!.first()

      assertThat(definition.name).isEqualTo("External Movements")
      assertThat(courtAndHospitalMovementsReport.name).isEqualTo("Court And Hospital Movement DPD")
      assertThat(definition.description).isEqualTo("Reports about prisoner external movements")
      assertThat(definition.variants).hasSize(3)
      assertThat(definition.variants[0]).isNotNull
      assertThat(definition.variants[1]).isNotNull
      assertThat(definition.variants[2]).isNotNull

      val lastMonthVariant = definition.variants[0]

      assertThat(lastMonthVariant.id).isEqualTo("last-month")
      assertThat(lastMonthVariant.name).isEqualTo("Last month")
      assertThat(lastMonthVariant.description).isEqualTo("All movements in the past month")

      val lastWeekVariant = definition.variants[1]
      assertThat(lastWeekVariant.id).isEqualTo("last-week")
      assertThat(lastWeekVariant.description).isEqualTo("All movements in the past week")
      assertThat(lastWeekVariant.name).isEqualTo("Last week")

      val lastYearVariant = definition.variants[2]
      assertThat(lastYearVariant.id).isEqualTo("last-year")
      assertThat(lastYearVariant.description).isEqualTo("All movements in the past year")
      assertThat(lastYearVariant.name).isEqualTo("Last year")
    }
  }

  class ReportDefinitionListWithMetricsTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithMetrics.json" }
      }
    }

    @Test
    fun `Definition list contains the dashboard definition`() {
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
      assertThat(result.responseBody).hasSize(1)
      assertThat(result.responseBody).first().isNotNull
      assertThat(result.responseBody!![0].dashboards).isNotNull
      assertThat(result.responseBody!![0].dashboards).hasSize(1)
      assertThat(result.responseBody!![0].dashboards).isEqualTo(
        listOf(
          DashboardDefinitionSummary(
            id = "test-dashboard-1",
            name = "Test Dashboard 1",
            description = "Test Dashboard 1 Description",
          ),
        ),
      )
    }
  }

  class ClientReportDefinitionListTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.dataProductDefinitions.host") { "http://localhost:9999" }
      }
    }

    @Test
    fun `Definition list is returned as expected when the definitions are retrieved from a service endpoint call`() {
      val result = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/definitions")
            .queryParam("dataProductDefinitionsPath", "definitions/prisons/orphanage")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ReportDefinitionSummary>()
        .returnResult()

      assertThat(result.responseBody).isNotNull
      assertThat(result.responseBody).hasSize(1)
      assertThat(result.responseBody).first().isNotNull

      val definition = result.responseBody!!.first()

      assertThat(definition.name).isEqualTo("External Movements")
      assertThat(definition.description).isEqualTo("Reports about prisoner external movements")
      assertThat(definition.variants).hasSize(3)
      assertThat(definition.variants[0]).isNotNull
      assertThat(definition.variants[1]).isNotNull

      val lastMonthVariant = definition.variants[0]

      assertThat(lastMonthVariant.id).isEqualTo("last-month")
      assertThat(lastMonthVariant.name).isEqualTo("Last month")
      assertThat(lastMonthVariant.description).isEqualTo("All movements in the past month")

      val lastWeekVariant = definition.variants[1]
      assertThat(lastWeekVariant.id).isEqualTo("last-week")
      assertThat(lastWeekVariant.description).isEqualTo("All movements in the past week")
      assertThat(lastWeekVariant.name).isEqualTo("Last week")
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
      .expectBodyList<ReportDefinitionSummary>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(1)
    assertThat(result.responseBody).first().isNotNull

    val definition = result.responseBody!!.first()

    assertThat(definition.variants).hasSize(3)
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
      .expectBodyList<ReportDefinitionSummary>()
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
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<SingleVariantReportDefinition>()
        .returnResult()

      assertThat(result.responseBody as SingleVariantReportDefinition).isNotNull

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
      assertThat(lastMonthVariant.specification?.fields).hasSize(9)
      assertThat(lastMonthVariant.printable).isEqualTo(true)

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
                      "staticOptions": [
                        {
                          "name": "Transfer In from Other Establishment",
                          "display": "Transfer In from Other Establishment"
                        }
                      ],
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
                  "name": "Last month"
              },
              {
                  "description": "All movements in the past week",
                  "id": "last-week",
                  "name": "Last week"
              },
              {
                  "description": "All movements in the past year",
                  "id": "last-year",
                  "name": "Last year"
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

  class ReportDefinitionParametersListTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithParameters.json" }
      }
    }

    @Test
    fun `Single Definition with parameters is returned with the parameters converted to filters`() {
      try {
        prisonerRepository.save(ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848)
        externalMovementRepository.save(ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn)

        webTestClient.get()
          .uri { uriBuilder: UriBuilder ->
            uriBuilder
              .path("/definitions/external-movements-with-parameters/last-month")
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
            "id": "external-movements-with-parameters",
            "name": "External Movements",
            "description": "Reports about prisoner external movements",
            "variant": {
              "id": "last-month",
              "name": "Last month",
              "resourceName": "reports/external-movements-with-parameters/last-month",
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
                      "staticOptions": [
                        {
                          "name": "Transfer In from Other Establishment",
                          "display": "Transfer In from Other Establishment"
                        }
                      ],
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
                    "name": "prisoner_number",
                    "display": "Enter NOMS Number",
                    "filter": {
                      "type": "text",
                      "mandatory": true
                    },
                    "sortable": false,
                    "defaultsort": false,
                    "type": "string",
                    "mandatory": false,
                    "visible": false,
                    "calculated": false
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
  }
}
