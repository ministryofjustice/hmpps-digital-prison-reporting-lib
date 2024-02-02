package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.matching.UrlPattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.NO_DATA_WARNING_HEADER_NAME
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.WARNING_NO_ACTIVE_CASELOAD

class ConfiguredApiIntegrationTest : IntegrationTestBase() {
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

  @Test
  fun `Configured API returns value from the repository`() {
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

    assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/me/caseloads")).size).isEqualTo(1)
  }

  class ReportDefinitionListTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.dataProductDefinitions.host") { "http://localhost:9999" }
      }
    }

    @Test
    fun `Configured API returns value from the repository when the definitions are retrieved via the web client`() {
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
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${movementPrisoner4[DATE]}:00", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
        )

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/me/caseloads")).size).isEqualTo(1)
    }

    @Test
    fun `Configured API count returns the number of records when the definitions are retrieved via the web client`() {
      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Configured API calls the definitions service when the cache is disabled`() {
      val expectedJson = """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${movementPrisoner4[DATE]}:00", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ] """
      val reqPath = "/reports/external-movements/last-month"
      val definitionsServicePath = "/definitions/prisons/orphanage"

      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path(reqPath)
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
          expectedJson,
        )

      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path(reqPath)
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
          expectedJson,
        )

      wireMockServer.verify(2, RequestPatternBuilder(RequestMethod.GET, UrlPattern(EqualToPattern(definitionsServicePath), false)))
    }
  }

  class CachedReportDefinitionListTest : IntegrationTestBase() {

    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.dataProductDefinitions.host") { "http://localhost:9999" }
        registry.add("dpr.lib.dataProductDefinitions.cache.enabled") { "true" }
      }
    }

    @Test
    fun `Configured API returns value from the cache when it is enabled`() {
      val expectedJson = """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${movementPrisoner4[DATE]}:00", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ] """
      val reqPath = "/reports/external-movements/last-month"
      val definitionsServicePath = "/definitions/prisons/orphanage"

      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path(reqPath)
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
          expectedJson,
        )

      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path(reqPath)
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
          expectedJson,
        )

      wireMockServer.verify(1, RequestPatternBuilder(RequestMethod.GET, UrlPattern(EqualToPattern(definitionsServicePath), false)))
    }
  }

  class ConfiguredApiFormulaTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithFormula.json" }
      }
    }

    @Test
    fun `Configured API returns value from the repository formatting the fields with formulas correctly`() {
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
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
        )

      assertThat(wireMockServer.findAll(RequestPatternBuilder().withUrl("/me/caseloads")).size).isEqualTo(1)
    }
  }

  @Test
  fun `Configured API count returns the number of records`() {
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
  fun `Configured API count returns filtered value`(direction: String?, numberOfResults: Int) {
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
  fun `Configured API returns value matching the filters provided`() {
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
  fun `Configured API returns value matching the dynamic filters provided`() {
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
  fun `Configured API returns value matching the dynamic filters provided, with case insensitivity`() {
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
  fun `Configured API call without query params defaults to preset query params`() {
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
  fun `Configured API returns filtered values`(direction: String?, numberOfResults: Int) {
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
  fun `Configured API returns empty list and warning header if no active caseloads`() {
    wireMockServer.resetAll()
    wireMockServer.stubFor(
      WireMock.get("/me/caseloads").willReturn(
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
      .valueEquals(NO_DATA_WARNING_HEADER_NAME, WARNING_NO_ACTIVE_CASELOAD)
      .expectBody()
      .json("[]")
  }

  @Test
  fun `Configured API count returns zero and warning header if no active caseloads`() {
    wireMockServer.resetAll()
    wireMockServer.stubFor(
      WireMock.get("/me/caseloads").willReturn(
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
      .valueEquals(NO_DATA_WARNING_HEADER_NAME, WARNING_NO_ACTIVE_CASELOAD)
      .expectBody()
      .jsonPath("count").isEqualTo("0")
  }

  @Test
  fun `Configured API returns 400 for invalid selectedPage query param`() {
    requestWithQueryAndAssert400("selectedPage", 0, "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API returns 400 for invalid pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", 0, "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API returns 400 for invalid (wrong type) pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", "a", "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API returns 400 for invalid sortColumn query param`() {
    requestWithQueryAndAssert400("sortColumn", "nonExistentColumn", "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API returns 400 for invalid sortedAsc query param`() {
    requestWithQueryAndAssert400("sortedAsc", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API returns 400 for non-existent filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}abc", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API count returns 400 for non-existent filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}abc", "abc", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Configured API returns 400 for a report field which is not a filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}prisonNumber", "some name", "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API count returns 400 for a report field which is not a filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}prisonNumber", "some name", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Configured API returns 400 for invalid startDate query param`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}date$RANGE_FILTER_START_SUFFIX", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `External movements returns 400 for invalid endDate query param`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX", "b", "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API count returns 400 for invalid startDate query param`() {
    requestWithQueryAndAssert400("filters.startDate", "a", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Configured API count returns 400 for invalid endDate query param`() {
    requestWithQueryAndAssert400("filters.endDate", "17-12-2050", "/reports/external-movements/last-month/count")
  }

  private fun requestWithQueryAndAssert400(paramName: String, paramValue: Any, path: String) {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path(path)
          .queryParam(paramName, paramValue)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isBadRequest
  }
}
