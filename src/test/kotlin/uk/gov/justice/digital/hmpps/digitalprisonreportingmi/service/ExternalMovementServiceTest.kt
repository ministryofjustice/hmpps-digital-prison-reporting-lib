package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementFilter.DIRECTION
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementModel
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ExternalMovementPrisonerEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ExternalMovementRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ExternalMovementRepositoryCustomTest.AllPrisoners.prisoner5207
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ExternalMovementRepositoryCustomTest.AllPrisoners.prisoner8894
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementServiceTest.AllEntities.allExternalMovementEntities
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementServiceTest.AllModels.allExternalMovementModels
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Collections.singletonMap

class ExternalMovementServiceTest {

  private val externalMovementRepository: ExternalMovementRepository = mock<ExternalMovementRepository>()
  private val externalMovementService = ExternalMovementService(externalMovementRepository)

  @BeforeEach
  fun setup() {
    whenever(externalMovementRepository.list(any(), any(), any(), any(), any())).thenReturn(allExternalMovementEntities)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get the list of movements`() {
    val sortColumn = "date"
    val actual = externalMovementService.list(2, 2, sortColumn, true, singletonMap(DIRECTION, "in"))
    verify(externalMovementRepository, times(1)).list(2, 2, sortColumn, true, singletonMap(DIRECTION, "in"))
    assertEquals(allExternalMovementModels, actual)
  }

  @ParameterizedTest
  @CsvSource(
    "date, date",
    "time, timeOnly",
    "prisonNumber, prisoners.number",
    "direction, direction",
    "from, origin",
    "to, destination",
    "type, type",
    "reason, reason",
  )
  fun `should call the repository with the correctly mapped sort column`(domainColumn: String, dataColumn: String) {
    externalMovementService.list(2, 2, domainColumn, true, singletonMap(DIRECTION, "Out"))
    verify(externalMovementRepository, times(1)).list(2, 2, dataColumn, true, singletonMap(DIRECTION, "Out"))
  }

  @Test
  fun `should call the repository to sort by lastname, firstname when name is the sort column`() {
    externalMovementService.list(2, 2, "name", true, singletonMap(DIRECTION, "Out"))
    verify(externalMovementRepository, times(1)).list(2, 2, "lastname,firstname", true, singletonMap(DIRECTION, "Out"))

    externalMovementService.list(2, 2, "name", false, singletonMap(DIRECTION, "Out"))
    verify(externalMovementRepository, times(1)).list(2, 2, "lastname,firstname", true, singletonMap(DIRECTION, "Out"))
  }

  @Test
  fun `should throw an exception for unknown column name and not call the repository`() {
    assertThrows<ValidationException> { externalMovementService.list(2, 2, "randomColumn", true, singletonMap(DIRECTION, "in")) }
    verify(externalMovementRepository, times(0)).list(any(), any(), any(), any(), any())
  }

  object AllModels {
    val externalMovement1 = ExternalMovementModel(
      AllEntities.externalMovement1.number,
      prisoner8894.firstName,
      prisoner8894.lastName,
      LocalDate.of(2023, 1, 31),
      LocalTime.of(3, 1),
      "Ranby",
      "Kirkham",
      "In",
      "Admission",
      "Unconvicted Remand",
    )
    val externalMovement2 = ExternalMovementModel(
      AllEntities.externalMovement2.number,
      prisoner5207.firstName,
      prisoner5207.lastName,
      LocalDate.of(2023, 4, 25),
      LocalTime.of(12, 19),
      "Elmley",
      "Pentonville",
      "In",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val allExternalMovementModels = listOf(
      externalMovement1,
      externalMovement2,
    )
  }

  object AllEntities {
    val externalMovement1 = ExternalMovementPrisonerEntity(
      prisoner8894.number,
      prisoner8894.firstName,
      prisoner8894.lastName,
      LocalDateTime.of(2023, 1, 31, 0, 0, 0),
      LocalDateTime.of(2023, 1, 31, 3, 1, 0),
      "Ranby",
      "Kirkham",
      "In",
      "Admission",
      "Unconvicted Remand",
    )
    val externalMovement2 = ExternalMovementPrisonerEntity(
      prisoner5207.number,
      prisoner5207.firstName,
      prisoner5207.lastName,
      LocalDateTime.of(2023, 4, 25, 0, 0, 0),
      LocalDateTime.of(2023, 4, 25, 12, 19, 0),
      "Elmley",
      "Pentonville",
      "In",
      "Transfer",
      "Transfer In from Other Establishment",
    )
    val allExternalMovementEntities = listOf(
      externalMovement1,
      externalMovement2,
    )
  }
}
