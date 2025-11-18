package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ResponseHeader
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DATE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DESTINATION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DESTINATION_CODE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DIRECTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.ORIGIN
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.ORIGIN_CODE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.REASON
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.TYPE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisonerDestinationCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovement4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ExternalMovementEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.PrisonerEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.WARNING_NO_ACTIVE_CASELOAD
import java.time.LocalDateTime

class DataApiIntegrationTest : IntegrationTestBase() {
  companion object {
    fun dateTimeWithSeconds(dateTime: Any?) = """$dateTime:00"""

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  val caseloadsWithNoneActive: String = """
          {
            "username": "TESTUSER1",
            "active": true,
            "accountType": "GENERAL",
            "caseloads": [
              {
                "id": "WWI",
                "name": "WANDSWORTH (HMP)"
              }
            ]
          }
  """.trimIndent()

  /**
   * Nested test class so we can add a couple entries to the mock db just for these tests
   */
  @Nested
  inner class SortDirectionDataApiIntegrationTest {
    @BeforeEach
    fun init() {
      externalMovementRepository.save(externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.save(PrisonerEntity(9848, "DD105GF", "FirstName6", "LastName6", null))
    }

    @Test
    fun `Data API returns value using defaultSort on DPD with sortedAsc but no column`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortedAsc", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/users/me/caseloads")).size).isEqualTo(2)
    }

    @Test
    fun `Data API returns value using defaultSort on DPD with column override but no sortedAsc`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/users/me/caseloads")).size).isEqualTo(2)
    }

    @Test
    fun `Data API returns value using defaultSort on DPD with no column override and no sortDirection`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/users/me/caseloads")).size).isEqualTo(2)
    }

    @Test
    fun `Data API returns value using defaultSort on DPD but with an override`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .queryParam("sortedAsc", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/users/me/caseloads")).size).isEqualTo(2)
    }
  }

  @Test
  fun `Data API returns value from the repository`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("selectedPage", 1)
          .queryParam("pageSize", 3)
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${dateTimeWithSeconds(movementPrisoner4[DATE])}", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
      )

    assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/users/me/caseloads")).size).isEqualTo(2)
  }

  class ConfiguredApiFormulaTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithFormula.json" }
        registry.add("URL_ENV_SUFFIX") { "dev" }
      }
    }

    @Test
    fun `Data API returns value from the repository formatting the fields with formulas correctly`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .queryParam("sortedAsc", false)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
          """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "01/05/2023", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN]}", 
        "destination": "<a href='https://prisoner-dev.digital.prison.service.justice.gov.uk/prisoner/${movementPrisoner4[PRISON_NUMBER]}' target=\"_blank\">${movementPrisoner4[NAME]}</a>", 
        "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
        )

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/users/me/caseloads")).size).isEqualTo(2)
    }

    @Test
    fun `Data API returns empty String as the value from the repository for a field which has a formula and whose result set value is null`() {
      externalMovementRepository.delete(externalMovement4)
      val externalMovement4WithNullType = ExternalMovementEntity(
        4,
        7849,
        LocalDateTime.of(2023, 5, 1, 0, 0, 0),
        LocalDateTime.of(2023, 5, 1, 15, 19, 0),
        null,
        "LWSTMC",
        "WANDSWORTH (HMP)",
        "WWI",
        "Out",
        "Transfer",
        "Transfer Out to Other Establishment",
      )
      externalMovementRepository.save(externalMovement4WithNullType)
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .queryParam("sortedAsc", false)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
          """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "01/05/2023", 
        "origin": null, "origin_code": "", 
        "destination": "<a href='https://prisoner-dev.digital.prison.service.justice.gov.uk/prisoner/${movementPrisoner4[PRISON_NUMBER]}' target=\"_blank\">${movementPrisoner4[NAME]}</a>", 
        "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
        )

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/users/me/caseloads")).size).isEqualTo(2)
    }
  }

  @Test
  fun `Data API count returns the number of records`() {
    webTestClient.get()
      .uri("/reports/external-movements/last-month/count")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("count").isEqualTo("1")
  }

  @ParameterizedTest
  @CsvSource(
    "In,  0",
    "Out, 1",
    ",    1",
  )
  fun `Data API count returns filtered value`(direction: String?, numberOfResults: Int) {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/count")
          .queryParam("filters.direction", direction?.lowercase())
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .jsonPath("count").isEqualTo(numberOfResults.toString())
  }

  @Test
  fun `Data API returns value matching the filters provided`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_START_SUFFIX", "2023-04-25")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX", "2023-05-20")
          .queryParam("${FILTERS_PREFIX}direction", "out")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """[
         {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${dateTimeWithSeconds(movementPrisoner4[DATE])}",
          "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
          "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
      )
  }

  @Test
  fun `Data API returns value matching the dynamic filters provided`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-week/name")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_START_SUFFIX", "2023-04-25")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX", "2023-05-20")
          .queryParam("${FILTERS_PREFIX}direction", "out")
          .queryParam("prefix", "La")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """
        [
          "${movementPrisoner4[NAME]}"
        ]
      """,
      )
  }

  @Test
  fun `Data API returns value matching the dynamic filters provided, with case insensitivity`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-week/name")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_START_SUFFIX", "2023-04-25")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX", "2023-05-20")
          .queryParam("${FILTERS_PREFIX}direction", "out")
          .queryParam("prefix", "la")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """
        [
          "${movementPrisoner4[NAME]}"
        ]
      """,
      )
  }

  @Test
  fun `Data API call without query params defaults to preset query params`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        """
      [
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${dateTimeWithSeconds(movementPrisoner4[DATE])}", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}",
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]
      """,
      )
  }

  @ParameterizedTest
  @CsvSource(
    "in,  0",
    "In,  0",
    "out, 1",
    "Out, 1",
    ",    1",
  )
  fun `Data API returns filtered values`(direction: String?, numberOfResults: Int) {
    val results = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("${FILTERS_PREFIX}direction", direction)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBodyList<Map<String, Any>>()
      .hasSize(numberOfResults)
      .returnResult()
      .responseBody

    if (direction != null) {
      results?.forEach {
        assertThat(it["direction"].toString().lowercase()).isEqualTo(direction.lowercase())
      }
    }
  }

  @Test
  fun `Data API returns empty list and warning header if no active caseloads`() {
    wireMockServer.resetAll()
    wireMockServer.stubFor(
      WireMock.get("/users/me/caseloads").willReturn(
        WireMock.aResponse()
          .withStatus(HttpStatus.OK.value())
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withBody(caseloadsWithNoneActive),
      ),
    )
    stubDefinitionsResponse()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("selectedPage", 1)
          .queryParam("pageSize", 3)
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .valueEquals(ResponseHeader.NO_DATA_WARNING_HEADER_NAME, WARNING_NO_ACTIVE_CASELOAD)
      .expectBody()
      .json("[]")
  }

  @Test
  fun `Data API count returns zero and warning header if no active caseloads`() {
    wireMockServer.resetAll()
    wireMockServer.stubFor(
      WireMock.get("/users/me/caseloads").willReturn(
        WireMock.aResponse()
          .withStatus(HttpStatus.OK.value())
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withBody(caseloadsWithNoneActive),
      ),
    )
    stubDefinitionsResponse()

    webTestClient.get()
      .uri("/reports/external-movements/last-month/count")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .valueEquals(ResponseHeader.NO_DATA_WARNING_HEADER_NAME, WARNING_NO_ACTIVE_CASELOAD)
      .expectBody()
      .jsonPath("count").isEqualTo("0")
  }

  @Test
  fun `Data API returns 400 for invalid selectedPage query param`() {
    requestWithQueryAndAssert400("selectedPage", 0, "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", 0, "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid (wrong type) pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", "a", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid sortColumn query param`() {
    requestWithQueryAndAssert400("sortColumn", "nonExistentColumn", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid sortedAsc query param`() {
    requestWithQueryAndAssert400("sortedAsc", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for non-existent filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}abc", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API count returns 400 for non-existent filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}abc", "abc", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Data API returns 400 for a report field which is not a filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}destination", "some name", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API count returns 400 for a report field which is not a filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}destination", "some name", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Data API returns 400 for invalid startDate query param`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}date$RANGE_FILTER_START_SUFFIX", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `External movements returns 400 for invalid endDate query param`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX", "b", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API count returns 400 for invalid startDate query param`() {
    requestWithQueryAndAssert400("filters.startDate", "a", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Data API count returns 400 for invalid endDate query param`() {
    requestWithQueryAndAssert400("filters.endDate", "17-12-2050", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `External movements returns 400 for missing mandatory filter query param`() {
    val params = mapOf(
      "${FILTERS_PREFIX}origin" to "AAA",
      "${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX" to "",
    )
    requestWithQueryAndAssert400(params, "/reports/external-movements/last-year")
  }

  @Test
  fun `External movements returns 400 for filter query param that doesn't match pattern`() {
    val params = mapOf(
      "${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX" to "2000-01-02",
      "${FILTERS_PREFIX}origin" to "Invalid",
    )
    requestWithQueryAndAssert400(params, "/reports/external-movements/last-year")
  }

  @Test
  fun `External movements returns 200 for filter query param that matches pattern`() {
    val params = mapOf(
      "${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX" to "2000-01-02",
      "${FILTERS_PREFIX}origin" to "AAA",
    )
    requestWithQueryAndAssert200(params, "/reports/external-movements/last-year")
  }

  private fun requestWithQueryAndAssert400(paramName: String, paramValue: Any, path: String) {
    requestWithQueryAndAssert400(mapOf(paramName to paramValue), path)
  }

  private fun requestWithQueryAndAssert400(params: Map<String, Any>, path: String) {
    requestWithQueryAndAssert(params, path).isBadRequest
  }

  private fun requestWithQueryAndAssert200(params: Map<String, Any>, path: String) {
    requestWithQueryAndAssert(params, path).isOk
  }

  private fun requestWithQueryAndAssert(params: Map<String, Any>, path: String): StatusAssertions = webTestClient.get()
    .uri { uriBuilder: UriBuilder ->
      params.entries.forEach { uriBuilder.queryParam(it.key, it.value) }

      uriBuilder.path(path).build()
    }
    .headers(setAuthorisation(roles = listOf(authorisedRole)))
    .exchange()
    .expectStatus()
}
