package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementModel
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ExternalMovementPrisonerEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ExternalMovementRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.date
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.destination
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.direction
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.name
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.origin
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.prisonNumber
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.reason
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.timeOnly
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService.SortingColumns.type

@Service
data class ExternalMovementService(val externalMovementRepository: ExternalMovementRepository) {

  fun list(selectedPage: Long, pageSize: Long, sortColumn: String, sortedAsc: Boolean, filters: Map<ExternalMovementFilter, Any>): List<ExternalMovementModel> {
    return externalMovementRepository.list(selectedPage, pageSize, validateAndMapSortColumn(sortColumn), sortedAsc, filters).map { e -> toModel(e) }
  }

  fun count(filters: Map<ExternalMovementFilter, Any>): Count {
    return Count(externalMovementRepository.count(filters))
  }

  private fun toModel(entity: ExternalMovementPrisonerEntity): ExternalMovementModel {
    return ExternalMovementModel(
      entity.number, entity.firstName, entity.lastName,
      entity.date.toLocalDate(), entity.time.toLocalTime(), entity.origin, entity.destination, entity.direction, entity.type, entity.reason,
    )
  }

  private fun validateAndMapSortColumn(sortColumn: String): String {
    return when (sortColumn) {
      "date" -> date
      "time" -> timeOnly
      "prisonNumber" -> prisonNumber
      "direction" -> direction
      "from" -> origin
      "to" -> destination
      "type" -> type
      "reason" -> reason
      "name" -> name
      else -> throw ValidationException("Invalid sort column $sortColumn")
    }
  }

  object SortingColumns {
    const val date = "date"
    const val timeOnly = "timeOnly"
    const val prisonNumber = "prisoners.number"
    const val direction = "direction"
    const val origin = "origin"
    const val destination = "destination"
    const val type = "type"
    const val reason = "reason"
    const val name = "lastname,firstname"
  }
}
