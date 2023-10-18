package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner1
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner2
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner3
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner5
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.allExternalMovements
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllPrisoners.allPrisoners
import java.time.LocalDateTime
import java.util.Collections

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConfiguredApiRepositoryTest {

  @Autowired
  lateinit var externalMovementRepository: ExternalMovementRepository

  @Autowired
  lateinit var prisonerRepository: PrisonerRepository

  @Autowired
  lateinit var configuredApiRepository: ConfiguredApiRepository

  @BeforeEach
  fun setup() {
    allExternalMovements.forEach {
      externalMovementRepository.save(it)
    }
    allPrisoners.forEach {
      prisonerRepository.save(it)
    }
  }

  val query = "SELECT " +
    "prisoners.number AS prisonNumber," +
    "CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name," +
    "movements.date," +
    "movements.direction," +
    "movements.type," +
    "movements.origin," +
    "movements.destination," +
    "movements.reason\n" +
    "FROM datamart.domain.movements_movements as movements\n" +
    "JOIN datamart.domain.prisoner_prisoner as prisoners\n" +
    "ON movements.prisoner = prisoners.id"

  private val caseloads = listOf("BOLTCC", "WII", "NSI", "LEICCC", "PTI")

  @Test
  fun `should return 2 external movements for the selected page 2 and pageSize 2 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 2, 2, "date", true, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner3, movementPrisoner4), actual)
    Assertions.assertEquals(2, actual.size)
  }

  @Test
  fun `should return 1 row for the selected page 3 and pageSize 2 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 3, 2, "date", true, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner5), actual)
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return 5 rows for the selected page 1 and pageSize 5 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 1, 5, "date", true, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner1, movementPrisoner2, movementPrisoner3, movementPrisoner4, movementPrisoner5), actual)
    Assertions.assertEquals(5, actual.size)
  }

  @Test
  fun `should return an empty list for the selected page 2 and pageSize 5 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 2, 5, "date", true, caseloads)
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should return an empty list for the selected page 6 and pageSize 1 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 6, 1, "date", true, caseloads)
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by date when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "date", expectedForAscending = movementPrisoner1, expectedForDescending = movementPrisoner5)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by prisoner when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "prisonNumber", expectedForAscending = movementPrisoner1, expectedForDescending = movementPrisoner3)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'origin' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "origin", expectedForAscending = movementPrisoner3, expectedForDescending = movementPrisoner1)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'destination' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "destination", expectedForAscending = movementPrisoner5, expectedForDescending = movementPrisoner4)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'direction' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "direction", expectedForAscending = movementPrisoner1, expectedForDescending = movementPrisoner4)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'type' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "type", expectedForAscending = movementPrisoner1, expectedForDescending = movementPrisoner2)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'reason' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "reason", expectedForAscending = movementPrisoner2, expectedForDescending = movementPrisoner1)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'lastname,firstname' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "name", expectedForAscending = movementPrisoner1, expectedForDescending = movementPrisoner4)

  @Test
  fun `should return a list of all results with no filters`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 1, 20, "date", true, caseloads)
    Assertions.assertEquals(5, actual.size)
  }

  @Test
  fun `should return a list of rows filtered by an in direction filter`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), Collections.singletonMap("direction", "In"), 1, 20, "date", true, caseloads)
    Assertions.assertEquals(4, actual.size)
  }

  @Test
  fun `should return a list of inwards movements with an in direction filter regardless of the casing`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), Collections.singletonMap("direction", "in"), 1, 20, "date", true, caseloads)
    Assertions.assertEquals(4, actual.size)
  }

  @Test
  fun `should return a list of rows filtered by out direction filter`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), Collections.singletonMap("direction", "Out"), 1, 20, "date", true, caseloads)
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return a list of outwards movements with an out direction filter regardless of the casing`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), Collections.singletonMap("direction", "out"), 1, 20, "date", true, caseloads)
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return all the rows on or after the provided start date`() {
    val actual = configuredApiRepository.executeQuery(query, Collections.singletonMap("date$RANGE_FILTER_START_SUFFIX", "2023-04-30"), emptyMap(), 1, 10, "date", false, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner5, movementPrisoner4, movementPrisoner3), actual)
  }

  @Test
  fun `should return all the rows on or before the provided end date`() {
    val actual = configuredApiRepository.executeQuery(query, Collections.singletonMap("date$RANGE_FILTER_END_SUFFIX", "2023-04-25"), emptyMap(), 1, 10, "date", false, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner2, movementPrisoner1), actual)
  }

  @Test
  fun `should return all the rows between the provided start and end dates`() {
    val actual = configuredApiRepository.executeQuery(query, mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-05-20"), emptyMap(), 1, 10, "date", false, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner5, movementPrisoner4, movementPrisoner3, movementPrisoner2), actual)
  }

  @Test
  fun `should return all the rows between the provided start and end dates matching the direction filter`() {
    val actual = configuredApiRepository.executeQuery(query, mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-05-20"), mapOf("direction" to "in"), 1, 10, "date", false, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner5, movementPrisoner3, movementPrisoner2), actual)
  }

  @Test
  fun `should return no rows if the start date is after the latest table date`() {
    val actual = configuredApiRepository.executeQuery(query, mapOf("date$RANGE_FILTER_START_SUFFIX" to "2025-01-01"), emptyMap(), 1, 10, "date", false, caseloads)
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should return no rows if the end date is before the earliest table date`() {
    val actual = configuredApiRepository.executeQuery(query, mapOf("date$RANGE_FILTER_END_SUFFIX" to "2015-01-01"), emptyMap(), 1, 10, "date", false, caseloads)
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should return no rows if the start date is after the end date`() {
    val actual = configuredApiRepository.executeQuery(query, mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-05-01", "date$RANGE_FILTER_END_SUFFIX" to "2023-04-25"), emptyMap(), 1, 10, "date", false, caseloads)
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should throw an exception if a range filter does not have a start or end suffix`() {
    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiRepository.executeQuery(query, mapOf("date" to "2023-05-01"), emptyMap(), 1, 10, "date", false, caseloads)
    }
    Assertions.assertEquals("Range filter does not have a .start or .end suffix: date", e.message)
  }

  @Test
  fun `should not throw an error when some columns are null`() {
    val externalMovementNullValues = ExternalMovementEntity(
      6,
      9846,
      LocalDateTime.of(2050, 6, 1, 0, 0, 0),
      LocalDateTime.of(2050, 6, 1, 12, 0, 0),
      "BOLTCC",
      null,
      null,
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val prisoner9846 = PrisonerEntity(9846, "W2505GF", "FirstName6", "LastName6", null)

    val movementPrisonerNullValues = mapOf(
      AllMovementPrisoners.PRISON_NUMBER to "W2505GF",
      AllMovementPrisoners.NAME to "LastName6, F",
      AllMovementPrisoners.DATE to "2050-06-01",
      AllMovementPrisoners.DIRECTION to null,
      AllMovementPrisoners.TYPE to "Transfer",
      AllMovementPrisoners.ORIGIN to "BOLTCC",
      AllMovementPrisoners.DESTINATION to null,
      AllMovementPrisoners.REASON to "Transfer In from Other Establishment",
    )
    try {
      externalMovementRepository.save(externalMovementNullValues)
      prisonerRepository.save(prisoner9846)
      val actual = configuredApiRepository.executeQuery(
        query,
        mapOf("date$RANGE_FILTER_START_SUFFIX" to "2050-06-01", "date$RANGE_FILTER_END_SUFFIX" to "2050-06-01"),
        emptyMap(),
        1,
        1,
        "date",
        true,
        caseloads,
      )
      Assertions.assertEquals(listOf(movementPrisonerNullValues), actual)
      Assertions.assertEquals(1, actual.size)
    } finally {
      externalMovementRepository.delete(externalMovementNullValues)
      prisonerRepository.delete(prisoner9846)
    }
  }

  @Test
  fun `should return only the rows whose origin or destination is in the caseloads list`() {
    val caseloads = listOf("LWSTMC")
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 1, 5, "date", true, caseloads)
    Assertions.assertEquals(listOf(movementPrisoner4), actual)
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return no rows for an empty caseloads list`() {
    val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 1, 5, "date", true, emptyList())
    Assertions.assertEquals(emptyList<Map<String, String>>(), actual)
    Assertions.assertEquals(0, actual.size)
  }

  @Test
  fun `should return a count of all rows with no filters`() {
    val actual = configuredApiRepository.count(emptyMap(), emptyMap(), query, caseloads)
    Assertions.assertEquals(5L, actual)
  }

  @Test
  fun `should return a count of rows with an in direction filter`() {
    val actual = configuredApiRepository.count(emptyMap(), Collections.singletonMap("direction", "in"), query, caseloads)
    Assertions.assertEquals(4L, actual)
  }

  @Test
  fun `should return a count of rows with an out direction filter`() {
    val actual = configuredApiRepository.count(emptyMap(), Collections.singletonMap("direction", "out"), query, caseloads)
    Assertions.assertEquals(1L, actual)
  }

  @Test
  fun `should return a count of rows with a startDate filter`() {
    val actual = configuredApiRepository.count(Collections.singletonMap("date$RANGE_FILTER_START_SUFFIX", "2023-05-01"), emptyMap(), query, caseloads)
    Assertions.assertEquals(2, actual)
  }

  @Test
  fun `should return a count of rows with an endDate filter`() {
    val actual = configuredApiRepository.count(Collections.singletonMap("date$RANGE_FILTER_END_SUFFIX", "2023-01-31"), emptyMap(), query, caseloads)
    Assertions.assertEquals(1, actual)
  }

  @Test
  fun `should return a count of movements with a startDate and an endDate filter`() {
    val actual = configuredApiRepository.count(mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-30", "date$RANGE_FILTER_END_SUFFIX" to "2023-05-01"), emptyMap(), query, caseloads)
    Assertions.assertEquals(2, actual)
  }

  @Test
  fun `should return a count of zero with a date start greater than the latest movement date`() {
    val actual = configuredApiRepository.count(Collections.singletonMap("date$RANGE_FILTER_START_SUFFIX", "2025-04-30"), emptyMap(), query, caseloads)
    Assertions.assertEquals(0, actual)
  }

  @Test
  fun `should return a count of zero with a date end less than the earliest movement date`() {
    val actual = configuredApiRepository.count(Collections.singletonMap("date$RANGE_FILTER_END_SUFFIX", "2019-04-30"), emptyMap(), query, caseloads)
    Assertions.assertEquals(0, actual)
  }

  @Test
  fun `should return a count of zero if the start date is after the end date`() {
    val actual = configuredApiRepository.count(mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-30", "date$RANGE_FILTER_END_SUFFIX" to "2019-05-01"), emptyMap(), query, caseloads)
    Assertions.assertEquals(0, actual)
  }

  private fun assertExternalMovements(sortColumn: String, expectedForAscending: Map<String, String>, expectedForDescending: Map<String, String>): List<DynamicTest> {
    return listOf(
      true to listOf(expectedForAscending),
      false to listOf(expectedForDescending),
    )
      .map { (sortedAsc, expected) ->
        DynamicTest.dynamicTest("When sorting by $sortColumn and sortedAsc is $sortedAsc the result is $expected") {
          val actual = configuredApiRepository.executeQuery(query, emptyMap(), emptyMap(), 1, 1, sortColumn, sortedAsc, caseloads)
          Assertions.assertEquals(expected, actual)
          Assertions.assertEquals(1, actual.size)
        }
      }
  }

  object AllMovements {
    val externalMovement1 = ExternalMovementEntity(
      1,
      8894,
      LocalDateTime.of(2023, 1, 31, 0, 0, 0),
      LocalDateTime.of(2023, 1, 31, 3, 1, 0),
      "PTI",
      "TCI",
      "In",
      "Admission",
      "Unconvicted Remand",
    )
    val externalMovement2 = ExternalMovementEntity(
      2,
      5207,
      LocalDateTime.of(2023, 4, 25, 0, 0, 0),
      LocalDateTime.of(2023, 4, 25, 12, 19, 0),
      "LEICCC",
      "LCI",
      "In",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val externalMovement3 = ExternalMovementEntity(
      3,
      4800,
      LocalDateTime.of(2023, 4, 30, 0, 0, 0),
      LocalDateTime.of(2023, 4, 30, 13, 19, 0),
      "BFI",
      "NSI",
      "In",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val externalMovement4 = ExternalMovementEntity(
      4,
      7849,
      LocalDateTime.of(2023, 5, 1, 0, 0, 0),
      LocalDateTime.of(2023, 5, 1, 15, 19, 0),
      "LWSTMC",
      "WII",
      "Out",
      "Transfer",
      "Transfer Out to Other Establishment",
    )
    val externalMovement5 = ExternalMovementEntity(
      5,
      6851,
      LocalDateTime.of(2023, 5, 20, 0, 0, 0),
      LocalDateTime.of(2023, 5, 20, 14, 0, 0),
      "BOLTCC",
      "HEI",
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
    val prisoner8894 = mapOf("id" to 8894, "number" to "G2504UV", "firstName" to "FirstName2", "lastName" to "LastName1", "livingUnitReference" to null)

    val prisoner5207 = mapOf("id" to 5207, "number" to "G2927UV", "firstName" to "FirstName1", "lastName" to "LastName1", "livingUnitReference" to null)

    val prisoner4800 = mapOf("id" to 4800, "number" to "G3418VR", "firstName" to "FirstName3", "lastName" to "LastName3", "livingUnitReference" to null)

    val prisoner7849 = mapOf("id" to 7849, "number" to "G3411VR", "firstName" to "FirstName4", "lastName" to "LastName5", "livingUnitReference" to 142595)

    val prisoner6851 = mapOf("id" to 6851, "number" to "G3154UG", "firstName" to "FirstName5", "lastName" to "LastName5", "livingUnitReference" to null)

    val allPrisoners = listOf(
      PrisonerEntity(8894, "G2504UV", "FirstName2", "LastName1", null),
      PrisonerEntity(5207, "G2927UV", "FirstName1", "LastName1", null),
      PrisonerEntity(4800, "G3418VR", "FirstName3", "LastName3", null),
      PrisonerEntity(7849, "G3411VR", "FirstName4", "LastName5", 142595),
      PrisonerEntity(6851, "G3154UG", "FirstName5", "LastName5", null),
    )
  }

  object AllMovementPrisoners {
    const val PRISON_NUMBER = "PRISONNUMBER"
    const val NAME = "NAME"
    const val DATE = "DATE"
    const val DIRECTION = "DIRECTION"
    const val TYPE = "TYPE"
    const val ORIGIN = "ORIGIN"
    const val DESTINATION = "DESTINATION"
    const val REASON = "REASON"

    val movementPrisoner1 = mapOf(PRISON_NUMBER to "G2504UV", NAME to "LastName1, F", DATE to "2023-01-31", DIRECTION to "In", TYPE to "Admission", ORIGIN to "PTI", DESTINATION to "TCI", REASON to "Unconvicted Remand")

    val movementPrisoner2 = mapOf(PRISON_NUMBER to "G2927UV", NAME to "LastName1, F", DATE to "2023-04-25", DIRECTION to "In", TYPE to "Transfer", ORIGIN to "LEICCC", DESTINATION to "LCI", REASON to "Transfer In from Other Establishment")

    val movementPrisoner3 = mapOf(PRISON_NUMBER to "G3418VR", NAME to "LastName3, F", DATE to "2023-04-30", DIRECTION to "In", TYPE to "Transfer", ORIGIN to "BFI", DESTINATION to "NSI", REASON to "Transfer In from Other Establishment")

    val movementPrisoner4 = mapOf(PRISON_NUMBER to "G3411VR", NAME to "LastName5, F", DATE to "2023-05-01", DIRECTION to "Out", TYPE to "Transfer", ORIGIN to "LWSTMC", DESTINATION to "WII", REASON to "Transfer Out to Other Establishment")

    val movementPrisoner5 = mapOf(PRISON_NUMBER to "G3154UG", NAME to "LastName5, F", DATE to "2023-05-20", DIRECTION to "In", TYPE to "Transfer", ORIGIN to "BOLTCC", DESTINATION to "HEI", REASON to "Transfer In from Other Establishment")
  }
}
