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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DATE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DESTINATION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DIRECTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.ORIGIN
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.REASON
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.TYPE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner1
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner2
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner3
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner5
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ExternalMovementEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ExternalMovementRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PrisonerEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PrisonerRepository
import java.time.LocalDateTime

class ConfiguredApiIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var externalMovementRepository: ExternalMovementRepository

  @Autowired
  lateinit var prisonerRepository: PrisonerRepository

  @BeforeEach
  fun setup() {
    AllMovements.allExternalMovements.forEach {
      externalMovementRepository.save(it)
    }
    AllPrisoners.allPrisoners.forEach {
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
        {"prisonNumber": "${movementPrisoner5[PRISON_NUMBER]}", "name": "${movementPrisoner5[NAME]}", "date": "${movementPrisoner5[DATE]}", "origin": "${movementPrisoner5[ORIGIN]}", "destination": "${movementPrisoner5[DESTINATION]}", "direction": "${movementPrisoner5[DIRECTION]}", "type": "${movementPrisoner5[TYPE]}", "reason": "${movementPrisoner5[REASON]}"},
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${movementPrisoner4[DATE]}", "origin": "${movementPrisoner4[ORIGIN]}", "destination": "${movementPrisoner4[DESTINATION]}", "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"},
        {"prisonNumber": "${movementPrisoner3[PRISON_NUMBER]}", "name": "${movementPrisoner3[NAME]}", "date": "${movementPrisoner3[DATE]}", "origin": "${movementPrisoner3[ORIGIN]}", "destination": "${movementPrisoner3[DESTINATION]}", "direction": "${movementPrisoner3[DIRECTION]}", "type": "${movementPrisoner3[TYPE]}", "reason": "${movementPrisoner3[REASON]}"}
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
      .jsonPath("count").isEqualTo("5")
  }

  @ParameterizedTest
  @CsvSource(
    "In,  4",
    "Out, 1",
    ",    5",
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
         {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${movementPrisoner4[DATE]}", "origin": "${movementPrisoner4[ORIGIN]}", "destination": "${movementPrisoner4[DESTINATION]}", "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
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
        {"prisonNumber": "${movementPrisoner5[PRISON_NUMBER]}", "name": "${movementPrisoner5[NAME]}", "date": "${movementPrisoner5[DATE]}", "origin": "${movementPrisoner5[ORIGIN]}", "destination": "${movementPrisoner5[DESTINATION]}", "direction": "${movementPrisoner5[DIRECTION]}", "type": "${movementPrisoner5[TYPE]}", "reason": "${movementPrisoner5[REASON]}"},
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${movementPrisoner4[DATE]}", "origin": "${movementPrisoner4[ORIGIN]}", "destination": "${movementPrisoner4[DESTINATION]}", "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"},
        {"prisonNumber": "${movementPrisoner3[PRISON_NUMBER]}", "name": "${movementPrisoner3[NAME]}", "date": "${movementPrisoner3[DATE]}", "origin": "${movementPrisoner3[ORIGIN]}", "destination": "${movementPrisoner3[DESTINATION]}", "direction": "${movementPrisoner3[DIRECTION]}", "type": "${movementPrisoner3[TYPE]}", "reason": "${movementPrisoner3[REASON]}"},
        {"prisonNumber": "${movementPrisoner2[PRISON_NUMBER]}", "name": "${movementPrisoner2[NAME]}", "date": "${movementPrisoner2[DATE]}", "origin": "${movementPrisoner2[ORIGIN]}", "destination": "${movementPrisoner2[DESTINATION]}", "direction": "${movementPrisoner2[DIRECTION]}", "type": "${movementPrisoner2[TYPE]}", "reason": "${movementPrisoner2[REASON]}"},
        {"prisonNumber": "${movementPrisoner1[PRISON_NUMBER]}", "name": "${movementPrisoner1[NAME]}", "date": "${movementPrisoner1[DATE]}", "origin": "${movementPrisoner1[ORIGIN]}", "destination": "${movementPrisoner1[DESTINATION]}", "direction": "${movementPrisoner1[DIRECTION]}", "type": "${movementPrisoner1[TYPE]}", "reason": "${movementPrisoner1[REASON]}"}
      ]
      """,
      )
  }

  @ParameterizedTest
  @CsvSource(
    "in,  4",
    "In,  4",
    "out, 1",
    "Out, 1",
    ",    5",
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
object AllMovements {
  val externalMovement1 = ExternalMovementEntity(
    1,
    8894,
    LocalDateTime.of(2023, 1, 31, 0, 0, 0),
    LocalDateTime.of(2023, 1, 31, 3, 1, 0),
    "Ranby",
    "Kirkham",
    "In",
    "Admission",
    "Unconvicted Remand",
  )
  val externalMovement2 = ExternalMovementEntity(
    2,
    5207,
    LocalDateTime.of(2023, 4, 25, 0, 0, 0),
    LocalDateTime.of(2023, 4, 25, 12, 19, 0),
    "Elmley",
    "Pentonville",
    "In",
    "Transfer",
    "Transfer In from Other Establishment",
  )
  val externalMovement3 = ExternalMovementEntity(
    3,
    4800,
    LocalDateTime.of(2023, 4, 30, 0, 0, 0),
    LocalDateTime.of(2023, 4, 30, 13, 19, 0),
    "Wakefield",
    "Dartmoor",
    "In",
    "Transfer",
    "Transfer In from Other Establishment",
  )
  val externalMovement4 = ExternalMovementEntity(
    4,
    7849,
    LocalDateTime.of(2023, 5, 1, 0, 0, 0),
    LocalDateTime.of(2023, 5, 1, 15, 19, 0),
    "Cardiff",
    "Maidstone",
    "Out",
    "Transfer",
    "Transfer Out to Other Establishment",
  )
  val externalMovement5 = ExternalMovementEntity(
    5,
    6851,
    LocalDateTime.of(2023, 5, 20, 0, 0, 0),
    LocalDateTime.of(2023, 5, 20, 14, 0, 0),
    "Isle of Wight",
    "Northumberland",
    "In",
    "Transfer",
    "Transfer In from Other Establishment",
  )
  val allExternalMovements = listOf(
    externalMovement1,
    externalMovement2,
    externalMovement3,
    externalMovement4,
    externalMovement5,
  )
}
object AllPrisoners {
  val prisoner8894 = PrisonerEntity(8894, "G2504UV", "FirstName2", "LastName1", null)
  val prisoner5207 = PrisonerEntity(5207, "G2927UV", "FirstName1", "LastName1", null)
  val prisoner4800 = PrisonerEntity(4800, "G3418VR", "FirstName3", "LastName3", null)
  val prisoner7849 = PrisonerEntity(7849, "G3411VR", "FirstName4", "LastName5", 142595)
  val prisoner6851 = PrisonerEntity(6851, "G3154UG", "FirstName5", "LastName5", null)

  val allPrisoners = listOf(
    prisoner8894,
    prisoner5207,
    prisoner4800,
    prisoner7849,
    prisoner6851,
  )
}
