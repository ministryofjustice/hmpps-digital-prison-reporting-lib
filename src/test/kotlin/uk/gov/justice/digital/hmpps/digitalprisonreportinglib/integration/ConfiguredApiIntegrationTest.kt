package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ExternalMovementRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PrisonerRepository

class ConfiguredApiIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var externalMovementRepository: ExternalMovementRepository

  @Autowired
  lateinit var prisonerRepository: PrisonerRepository

  @BeforeEach
  override fun setup() {
    super.setup()
    ConfiguredApiRepositoryTest.AllMovements.allExternalMovements.forEach {
      externalMovementRepository.save(it)
    }
    ConfiguredApiRepositoryTest.AllPrisoners.allPrisoners.forEach {
      prisonerRepository.save(it)
    }
  }

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
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "2023-05-01T15:19:00", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
      )
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
         {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "2023-05-01T15:19:00",
          "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
          "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
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
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "2023-05-01T15:19:00", 
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
    requestWithQueryAndAssert400("${FILTERS_PREFIX}name", "some name", "/reports/external-movements/last-month")
  }

  @Test
  fun `Configured API count returns 400 for a report field which is not a filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}name", "some name", "/reports/external-movements/last-month/count")
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
