package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementFilter.DIRECTION
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementFilter.END_DATE
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementFilter.START_DATE
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementModel
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService
import java.time.LocalDate

@Validated
@RestController
@Tag(name = "External Movements API")
class ExternalMovementsController(val externalMovementService: ExternalMovementService) {

  @GetMapping("/external-movements/count")
  @Operation(
    description = "Gets a count of external movements",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun count(
    @RequestParam direction: String?,
    @Parameter(description = "The start date (inclusive) from which to filter, in the format of yyyy-mm-dd.", example = "2023-04-25")
    @RequestParam
    startDate: LocalDate?,
    @Parameter(description = "The end date (inclusive) up to which to filter, in the format of yyyy-mm-dd.", example = "2023-04-25")
    @RequestParam
    endDate: LocalDate?,
  ): Count {
    return externalMovementService.count(createFilterMap(direction, startDate, endDate))
  }

  @GetMapping("/external-movements")
  @Operation(
    description = "Gets a list of external movements",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun externalMovements(
    @RequestParam(defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    @RequestParam(defaultValue = "10")
    @Min(1)
    pageSize: Long,
    @RequestParam(defaultValue = "date") sortColumn: String,
    @RequestParam(defaultValue = "false") sortedAsc: Boolean,
    @RequestParam
    @Parameter(description = "The direction to filter. It can be either In or Out", example = "in")
    direction: String?,
    @Parameter(description = "The start date (inclusive) from which to filter, in the format of yyyy-mm-dd.", example = "2023-04-25")
    @RequestParam
    startDate: LocalDate?,
    @Parameter(description = "The end date (inclusive) up to which to filter, in the format of yyyy-mm-dd.", example = "2023-04-25")
    @RequestParam
    endDate: LocalDate?,
  ): List<ExternalMovementModel> {
    return externalMovementService.list(
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      filters = createFilterMap(direction, startDate, endDate),
    )
  }

  private fun createFilterMap(direction: String?, startDate: LocalDate?, endDate: LocalDate?): Map<ExternalMovementFilter, Any> =
    buildMap {
      direction?.trim()?.let { if (it.isNotEmpty()) put(DIRECTION, it) }
      startDate?.let { put(START_DATE, it) }
      endDate?.let { put(END_DATE, it) }
    }
}
