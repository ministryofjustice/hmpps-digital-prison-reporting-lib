package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Filter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DATE_RANGE_END
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DATE_RANGE_START
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DYNAMIC
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner1
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner2
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner3
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner5
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisonerDestinationCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.allExternalMovements
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovement1
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovement2
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovement3
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovement4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovement5
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionOut
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementOriginCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllPrisoners.allPrisoners
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllPrisoners.prisoner9846
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllPrisoners.prisoner9847
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllPrisoners.prisoner9848
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConfiguredApiRepositoryTest {

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

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
    "movements.time AS date," +
    "movements.direction," +
    "movements.type," +
    "movements.origin," +
    "movements.origin_code," +
    "movements.destination," +
    "movements.destination_code," +
    "movements.reason\n" +
    "FROM datamart.domain.movement_movement as movements\n" +
    "JOIN datamart.domain.prisoner_prisoner as prisoners\n" +
    "ON movements.prisoner = prisoners.id"

  private val policyEngineResult = "(origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')"
  private val dataSourceName = "datamart"

  @Test
  fun `should return 2 external movements for the selected page 2 and pageSize 2 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = emptyList(),
      selectedPage = 2,
      pageSize = 2,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(listOf(movementPrisoner3, movementPrisoner4), actual)
    Assertions.assertEquals(2, actual.size)
  }

  @Test
  fun `should return 1 row for the selected page 3 and pageSize 2 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = emptyList(),
      selectedPage = 3,
      pageSize = 2,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(listOf(movementPrisoner5), actual)
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return 5 rows for the selected page 1 and pageSize 5 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = emptyList(),
      selectedPage = 1,
      pageSize = 5,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(listOf(movementPrisoner1, movementPrisoner2, movementPrisoner3, movementPrisoner4, movementPrisoner5), actual)
    Assertions.assertEquals(5, actual.size)
  }

  @Test
  fun `should return an empty list for the selected page 2 and pageSize 5 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = emptyList(),
      selectedPage = 2,
      pageSize = 5,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should return an empty list for the selected page 6 and pageSize 1 sorted by date in ascending order`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = emptyList(),
      selectedPage = 6,
      pageSize = 1,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
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
    assertExternalMovements(sortColumn = "origin", expectedForAscending = movementPrisoner3, expectedForDescending = movementPrisoner4)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'origin_code' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "origin_code", expectedForAscending = movementPrisoner3, expectedForDescending = movementPrisoner1)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'destination' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "destination", expectedForAscending = movementPrisoner5, expectedForDescending = movementPrisoner4)

  @TestFactory
  fun `should return all rows for the selected page and pageSize sorted by 'destination_code' when sortedAsc is true and when it is false`() =
    assertExternalMovements(sortColumn = "destination_code", expectedForAscending = movementPrisoner5, expectedForDescending = movementPrisoner4)

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
  fun `should return data and not error when there is no sort column provided `() {
    var actual: List<Map<String, Any>> = emptyList()
    assertDoesNotThrow {
      actual = configuredApiRepository.executeQuery(
        query = query,
        filters = emptyList(),
        selectedPage = 3,
        pageSize = 2,
        sortColumn = null,
        sortedAsc = true,
        reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
      )
    }
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return a list of all results with no filters`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = emptyList(),
      selectedPage = 1,
      pageSize = 20,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(5, actual.size)
  }

  @Test
  fun `should return a list of rows filtered by an in direction filter`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("direction", "In")),
      selectedPage = 1,
      pageSize = 20,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(4, actual.size)
  }

  @Test
  fun `should return a list of inwards movements with an in direction filter regardless of the casing`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("direction", "in")),
      selectedPage = 1,
      pageSize = 20,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(4, actual.size)
  }

  @Test
  fun `should return a list of rows filtered by out direction filter`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("direction", "Out")),
      selectedPage = 1,
      pageSize = 20,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return a list of outwards movements with an out direction filter regardless of the casing`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("direction", "out")),
      selectedPage = 1,
      pageSize = 20,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(1, actual.size)
  }

  @Test
  fun `should return all the rows on or after the provided start date`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("date", "2023-04-30", DATE_RANGE_START)),
      selectedPage = 1,
      pageSize = 10,
      sortColumn = "date",
      sortedAsc = false,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(listOf(movementPrisoner5, movementPrisoner4, movementPrisoner3), actual)
  }

  @Test
  fun `should return all the rows on or before the provided end date`() {
    val actual = configuredApiRepository.executeQuery(
      query,
      listOf(Filter("date", "2023-04-25", DATE_RANGE_END)),
      1,
      10,
      "date",
      false,
      EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(listOf(movementPrisoner2, movementPrisoner1), actual)
  }

  @Test
  fun `should return all the rows between the provided start and end dates`() {
    val actual = configuredApiRepository.executeQuery(
      query,
      listOf(Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-05-20", DATE_RANGE_END)),
      1,
      10,
      "date",
      false,
      EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(listOf(movementPrisoner5, movementPrisoner4, movementPrisoner3, movementPrisoner2), actual)
  }

  @Test
  fun `should return all the rows between the provided start and end dates matching the direction filter`() {
    val actual = configuredApiRepository.executeQuery(
      query,
      listOf(Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-05-20", DATE_RANGE_END), Filter("direction", "in")),
      1,
      10,
      "date",
      false,
      EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(listOf(movementPrisoner5, movementPrisoner3, movementPrisoner2), actual)
  }

  @Test
  fun `should return all the rows matching the dynamic filter between the provided start and end dates and given direction `() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(
        Filter("date", "2023-04-25", DATE_RANGE_START),
        Filter("date", "2023-05-20", DATE_RANGE_END),
        Filter("direction", "in"),
        Filter("name", "La", DYNAMIC),
      ),
      selectedPage = 1,
      pageSize = 10,
      sortColumn = NAME,
      sortedAsc = false,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dynamicFilterFieldId = NAME,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(
      listOf(
        mapOf(NAME to "LastName5, F"),
        mapOf(NAME to "LastName3, F"),
        mapOf(NAME to "LastName1, F"),
      ),
      actual,
    )
  }

  @Test
  fun `should return no the rows if the dynamic filter does not match anything`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(
        Filter("date", "2023-04-25", DATE_RANGE_START),
        Filter("date", "2023-05-20", DATE_RANGE_END),
        Filter("direction", "in"),
        Filter("name", "Ab", DYNAMIC),
      ),
      selectedPage = 1,
      pageSize = 10,
      sortColumn = "date",
      sortedAsc = false,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should return no rows if the start date is after the latest table date`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("date", "2025-01-01", DATE_RANGE_START)),
      selectedPage = 1,
      pageSize = 10,
      sortColumn = "date",
      sortedAsc = false,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should return no rows if the end date is before the earliest table date`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("date", "2015-01-01", DATE_RANGE_END)),
      selectedPage = 1,
      pageSize = 10,
      sortColumn = "date",
      sortedAsc = false,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should return no rows if the start date is after the end date`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("date", "2023-05-01", DATE_RANGE_START), Filter("date", "2023-04-25", DATE_RANGE_END)),
      selectedPage = 1,
      pageSize = 10,
      sortColumn = "date",
      sortedAsc = false,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(emptyList<Map<String, Any>>(), actual)
  }

  @Test
  fun `should not throw an error when some columns are null`() {
    val policyEngineResult = "(origin_code IN ('BOLTCC') AND lower(direction)='out') OR (destination_code IN ('BOLTCC') AND lower(direction)='in')"
    val externalMovementNullValues = ExternalMovementEntity(
      6,
      9846,
      LocalDateTime.of(2050, 6, 1, 0, 0, 0),
      LocalDateTime.of(2050, 6, 1, 12, 0, 0),
      "Bolton Crown Court",
      "BOLTCC",
      null,
      null,
      "Out",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val prisoner9846 = PrisonerEntity(9846, "W2505GF", "FirstName6", "LastName6", null)

    val movementPrisonerNullValues = mapOf(
      AllMovementPrisoners.PRISON_NUMBER to "W2505GF",
      AllMovementPrisoners.NAME to "LastName6, F",
      AllMovementPrisoners.DATE to externalMovementNullValues.time,
      AllMovementPrisoners.DIRECTION to "Out",
      AllMovementPrisoners.TYPE to "Transfer",
      AllMovementPrisoners.ORIGIN to "Bolton Crown Court",
      AllMovementPrisoners.ORIGIN_CODE to "BOLTCC",
      AllMovementPrisoners.DESTINATION to null,
      AllMovementPrisoners.DESTINATION_CODE to null,
      AllMovementPrisoners.REASON to "Transfer In from Other Establishment",
    )
    try {
      externalMovementRepository.save(externalMovementNullValues)
      prisonerRepository.save(prisoner9846)
      val actual = configuredApiRepository.executeQuery(
        query = query,
        filters = listOf(Filter("date", "2050-06-01", DATE_RANGE_START), Filter("date", "2050-06-01", DATE_RANGE_END)),
        selectedPage = 1,
        pageSize = 1,
        sortColumn = "date",
        sortedAsc = true,
        reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
      )
      Assertions.assertEquals(listOf(movementPrisonerNullValues), actual)
      Assertions.assertEquals(1, actual.size)
    } finally {
      externalMovementRepository.delete(externalMovementNullValues)
      prisonerRepository.delete(prisoner9846)
    }
  }

  @Test
  fun `should return only the rows whose origin code is in the caseloads list and its direction is "Out" or the destination code is in the caseloads list and its direction is "IN" for external-movements`() {
    try {
      val policyEngineResult = "(origin_code IN ('LWSTMC') AND lower(direction)='out') OR (destination_code IN ('LWSTMC') AND lower(direction)='in')"
      externalMovementRepository.save(externalMovementOriginCaseloadDirectionIn)
      externalMovementRepository.save(externalMovementDestinationCaseloadDirectionOut)
      externalMovementRepository.save(externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.save(prisoner9846)
      prisonerRepository.save(prisoner9847)
      prisonerRepository.save(prisoner9848)
      val actual = configuredApiRepository.executeQuery(
        query = query,
        filters = listOf(Filter("date", "2022-06-01", DATE_RANGE_START), Filter("date", "2024-06-01", DATE_RANGE_END)),
        selectedPage = 1,
        pageSize = 10,
        sortColumn = "date",
        sortedAsc = true,
        reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
      )
      Assertions.assertEquals(listOf(movementPrisoner4, movementPrisonerDestinationCaseloadDirectionIn), actual)
      Assertions.assertEquals(2, actual.size)
    } finally {
      externalMovementRepository.delete(externalMovementOriginCaseloadDirectionIn)
      externalMovementRepository.delete(externalMovementDestinationCaseloadDirectionOut)
      externalMovementRepository.delete(externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.delete(prisoner9846)
      prisonerRepository.delete(prisoner9847)
      prisonerRepository.delete(prisoner9848)
    }
  }

  @Test
  fun `should return no rows for a policy deny`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = emptyList(),
      selectedPage = 1,
      pageSize = 5,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = POLICY_DENY,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(emptyList<Map<String, String>>(), actual)
    Assertions.assertEquals(0, actual.size)
  }

  @Test
  fun `should return no rows for a policy deny even if some filters match`() {
    val actual = configuredApiRepository.executeQuery(
      query = query,
      filters = listOf(Filter("direction", "in")),
      selectedPage = 1,
      pageSize = 5,
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = POLICY_DENY,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(emptyList<Map<String, String>>(), actual)
    Assertions.assertEquals(0, actual.size)
  }

  @Test
  fun `should return a count of all rows with no filters`() {
    val actual = configuredApiRepository.count(
      filters = emptyList(),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(5L, actual)
  }

  @Test
  fun `should return a count of rows with an in direction filter`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("direction", "in")),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(4L, actual)
  }

  @Test
  fun `should return a count of rows with an out direction filter`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("direction", "out")),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(1L, actual)
  }

  @Test
  fun `should return a count of rows with a startDate filter`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("date", "2023-05-01", DATE_RANGE_START)),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(2, actual)
  }

  @Test
  fun `should return a count of rows with an endDate filter`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("date", "2023-01-31", DATE_RANGE_END)),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(1, actual)
  }

  @Test
  fun `should return a count of movements with a startDate and an endDate filter`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("date", "2023-04-30", DATE_RANGE_START), Filter("date", "2023-05-01", DATE_RANGE_END)),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(2, actual)
  }

  @Test
  fun `should return a count of zero with a date start greater than the latest movement date`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("date", "2025-04-30", DATE_RANGE_START)),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(0, actual)
  }

  @Test
  fun `should return a count of zero with a date end less than the earliest movement date`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("date", "2019-04-30", DATE_RANGE_END)),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(0, actual)
  }

  @Test
  fun `should return a count of zero if the start date is after the end date`() {
    val actual = configuredApiRepository.count(
      filters = listOf(Filter("date", "2023-04-30", DATE_RANGE_START), Filter("date", "2019-05-01", DATE_RANGE_END)),
      query = query,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
    )
    Assertions.assertEquals(0, actual)
  }

  private fun assertExternalMovements(sortColumn: String, expectedForAscending: Map<String, Any>, expectedForDescending: Map<String, Any>): List<DynamicTest> {
    return listOf(
      true to listOf(expectedForAscending),
      false to listOf(expectedForDescending),
    )
      .map { (sortedAsc, expected) ->
        DynamicTest.dynamicTest("When sorting by $sortColumn and sortedAsc is $sortedAsc the result is $expected") {
          val actual = configuredApiRepository.executeQuery(
            query = query,
            filters = emptyList(),
            selectedPage = 1,
            pageSize = 1,
            sortColumn = sortColumn,
            sortedAsc = sortedAsc,
            reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
            policyEngineResult = policyEngineResult,
            dataSourceName = dataSourceName,
          )
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
      "KINGSTON (HMP)",
      "PTI",
      "THORN CROSS (HMPYOI)",
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
      "Leicester Crown Court",
      "LEICCC",
      "LEICESTER (HMP)",
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
      "BEDFORD (HMP)",
      "BFI",
      "NORTH SEA CAMP (HMP)",
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
      "Lowestoft (North East Suffolk) Magistrat",
      "LWSTMC",
      "WANDSWORTH (HMP)",
      "WWI",
      "Out",
      "Transfer",
      "Transfer Out to Other Establishment",
    )
    val externalMovement5 = ExternalMovementEntity(
      5,
      6851,
      LocalDateTime.of(2023, 5, 20, 0, 0, 0),
      LocalDateTime.of(2023, 5, 20, 14, 0, 0),
      "Bolton Crown Court",
      "BOLTCC",
      "HMP HEWELL",
      "HEI",
      "In",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val externalMovementOriginCaseloadDirectionIn = ExternalMovementEntity(
      6,
      9846,
      LocalDateTime.of(2023, 6, 1, 0, 0, 0),
      LocalDateTime.of(2023, 6, 1, 12, 0, 0),
      "Lowestoft (North East Suffolk) Magistrat",
      "LWSTMC",
      "Manchester",
      "MNCH",
      "In",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val externalMovementDestinationCaseloadDirectionOut = ExternalMovementEntity(
      7,
      9847,
      LocalDateTime.of(2023, 6, 1, 0, 0, 0),
      LocalDateTime.of(2023, 6, 1, 12, 0, 0),
      "Manchester",
      "MNCH",
      "Lowestoft (North East Suffolk) Magistrat",
      "LWSTMC",
      "Out",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val externalMovementDestinationCaseloadDirectionIn = ExternalMovementEntity(
      8,
      9848,
      LocalDateTime.of(2023, 6, 1, 0, 0, 0),
      LocalDateTime.of(2023, 6, 1, 12, 0, 0),
      "Manchester",
      "MNCH",
      "Lowestoft (North East Suffolk) Magistrat",
      "LWSTMC",
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
    val prisoner9846 = PrisonerEntity(9846, "W2505GF", "FirstName6", "LastName6", null)
    val prisoner9847 = PrisonerEntity(9847, "AB905GF", "FirstName6", "LastName6", null)
    val prisoner9848 = PrisonerEntity(9848, "DD105GF", "FirstName6", "LastName6", null)

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
    const val ORIGIN_CODE = "ORIGIN_CODE"
    const val DESTINATION = "DESTINATION"
    const val DESTINATION_CODE = "DESTINATION_CODE"
    const val REASON = "REASON"

    val movementPrisoner1 = mapOf(PRISON_NUMBER to "G2504UV", NAME to "LastName1, F", DATE to externalMovement1.time, DIRECTION to "In", TYPE to "Admission", ORIGIN to "KINGSTON (HMP)", ORIGIN_CODE to "PTI", DESTINATION to "THORN CROSS (HMPYOI)", DESTINATION_CODE to "TCI", REASON to "Unconvicted Remand")

    val movementPrisoner2 = mapOf(PRISON_NUMBER to "G2927UV", NAME to "LastName1, F", DATE to externalMovement2.time, DIRECTION to "In", TYPE to "Transfer", ORIGIN to "Leicester Crown Court", ORIGIN_CODE to "LEICCC", DESTINATION to "LEICESTER (HMP)", DESTINATION_CODE to "LCI", REASON to "Transfer In from Other Establishment")

    val movementPrisoner3 = mapOf(PRISON_NUMBER to "G3418VR", NAME to "LastName3, F", DATE to externalMovement3.time, DIRECTION to "In", TYPE to "Transfer", ORIGIN to "BEDFORD (HMP)", ORIGIN_CODE to "BFI", DESTINATION to "NORTH SEA CAMP (HMP)", DESTINATION_CODE to "NSI", REASON to "Transfer In from Other Establishment")

    val movementPrisoner4 = mapOf(PRISON_NUMBER to "G3411VR", NAME to "LastName5, F", DATE to externalMovement4.time, DIRECTION to "Out", TYPE to "Transfer", ORIGIN to "Lowestoft (North East Suffolk) Magistrat", ORIGIN_CODE to "LWSTMC", DESTINATION to "WANDSWORTH (HMP)", DESTINATION_CODE to "WWI", REASON to "Transfer Out to Other Establishment")

    val movementPrisoner5 = mapOf(PRISON_NUMBER to "G3154UG", NAME to "LastName5, F", DATE to externalMovement5.time, DIRECTION to "In", TYPE to "Transfer", ORIGIN to "Bolton Crown Court", ORIGIN_CODE to "BOLTCC", DESTINATION to "HMP HEWELL", DESTINATION_CODE to "HEI", REASON to "Transfer In from Other Establishment")

    val movementPrisonerOriginCaseloadDirectionIn = mapOf(
      PRISON_NUMBER to "W2505GF",
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DIRECTION to "In",
      TYPE to "Transfer",
      ORIGIN to "Lowestoft (North East Suffolk) Magistrat",
      ORIGIN_CODE to "LWSTMC",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
      REASON to "Transfer In from Other Establishment",
    )
    val movementPrisonerDestinationCaseloadDirectionOut = mapOf(
      PRISON_NUMBER to "AB905GF",
      NAME to "LastName6, F",
      DATE to externalMovementDestinationCaseloadDirectionOut.time,
      DIRECTION to "Out",
      TYPE to "Transfer",
      ORIGIN to "Manchester",
      ORIGIN_CODE to "MNCH",
      DESTINATION to "Lowestoft (North East Suffolk) Magistrat",
      DESTINATION_CODE to "LWSTMC",
      REASON to "Transfer In from Other Establishment",
    )
    val movementPrisonerDestinationCaseloadDirectionIn = mapOf(
      PRISON_NUMBER to "DD105GF",
      NAME to "LastName6, F",
      DATE to externalMovementDestinationCaseloadDirectionIn.time,
      DIRECTION to "In",
      TYPE to "Transfer",
      ORIGIN to "Manchester",
      ORIGIN_CODE to "MNCH",
      DESTINATION to "Lowestoft (North East Suffolk) Magistrat",
      DESTINATION_CODE to "LWSTMC",
      REASON to "Transfer In from Other Establishment",
    )
  }
}
