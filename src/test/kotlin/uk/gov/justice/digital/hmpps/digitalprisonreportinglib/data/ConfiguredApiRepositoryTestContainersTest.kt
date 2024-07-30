package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConfiguredApiRepositoryTestContainersTest {

  @Autowired
  lateinit var externalMovementRepository: ExternalMovementRepository

  @Autowired
  lateinit var prisonerRepository: PrisonerRepository

  @Autowired
  lateinit var configuredApiRepository: ConfiguredApiRepository

  companion object {
    @DynamicPropertySource
    @JvmStatic
    fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
      registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl)
      registry.add("spring.datasource.username", postgreSQLContainer::getUsername)
      registry.add("spring.datasource.password", postgreSQLContainer::getPassword)
      registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName)
      registry.add("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect"::toString)
    }

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:latest")
  }

  private val policyEngineResult = "TRUE"
  private val dataSourceName = "datamart"

  @BeforeEach
  fun setup() {
    ConfiguredApiRepositoryTest.AllMovements.allExternalMovements.forEach {
      externalMovementRepository.save(it)
    }
    ConfiguredApiRepositoryTest.AllPrisoners.allPrisoners.forEach {
      prisonerRepository.save(it)
    }
  }

  @Test
  fun `should return all the rows that match the boolean filter`() {
    val externalMovementEntityWithBoolean = ExternalMovementEntity(
      16,
      7852,
      LocalDateTime.of(2023, 5, 20, 0, 0, 0),
      LocalDateTime.of(2023, 5, 20, 14, 0, 0),
      "Bolton Crown Court",
      "BOLTCC",
      "HMP HEWELL",
      "HEI",
      "In",
      "Transfer",
      "Transfer In from Other Establishment",
      isClosed = true,
    )
    val prisonerEntity = PrisonerEntity(7852, "G3154UG", "FirstName5", "LastName5", null)
    try {
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
        "movements.reason," +
        "movements.is_closed\n" +
        "FROM domain.movement_movement as movements\n" +
        "JOIN domain.prisoner_prisoner as prisoners\n" +
        "ON movements.prisoner = prisoners.id"
      externalMovementRepository.save(externalMovementEntityWithBoolean)
      prisonerRepository.save(prisonerEntity)
      val movementPrisoner = mapOf(
        ConfiguredApiRepositoryTest.AllMovementPrisoners.PRISON_NUMBER.lowercase() to "G3154UG",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.NAME.lowercase() to "LastName5, F",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.DATE.lowercase() to ConfiguredApiRepositoryTest.AllMovements.externalMovement5.time,
        ConfiguredApiRepositoryTest.AllMovementPrisoners.DIRECTION.lowercase() to "In",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.TYPE.lowercase() to "Transfer",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.ORIGIN.lowercase() to "Bolton Crown Court",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.ORIGIN_CODE.lowercase() to "BOLTCC",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.DESTINATION.lowercase() to "HMP HEWELL",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.DESTINATION_CODE.lowercase() to "HEI",
        ConfiguredApiRepositoryTest.AllMovementPrisoners.REASON.lowercase() to "Transfer In from Other Establishment",
        "is_closed" to true,
      )
      val productDefinition = mock<SingleReportProductDefinition>()
      whenever(productDefinition.report).thenReturn(mock<Report>())
      val actual = configuredApiRepository.executeQuery(
        query,
        listOf(ConfiguredApiRepository.Filter("is_closed", "true", RepositoryHelper.FilterType.BOOLEAN)),
        1,
        10,
        "date",
        false,
        EXTERNAL_MOVEMENTS_PRODUCT_ID,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = productDefinition,
      )
      Assertions.assertEquals(listOf(movementPrisoner), actual)
    } finally {
      externalMovementRepository.delete(externalMovementEntityWithBoolean)
      prisonerRepository.delete(prisonerEntity)
    }
  }
}
