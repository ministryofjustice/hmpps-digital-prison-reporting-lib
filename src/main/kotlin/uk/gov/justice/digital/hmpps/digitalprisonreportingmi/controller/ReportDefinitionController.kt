package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ReportDefinitionService

@Validated
@RestController
@Tag(name = "Report Definition API")
class ReportDefinitionController(val reportDefinitionService: ReportDefinitionService) {

  @GetMapping("/definitions")
  @Operation(
    description = "Gets all report definitions",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun definitions(
    @Parameter(
      description = "Set this parameter to filter the list to only include reports for the given rendering method.",
      example = "HTML",
    )
    @RequestParam("renderMethod")
    renderMethod: RenderMethod?,
  ): List<ReportDefinition> {
    return reportDefinitionService.getListForUser(renderMethod)
  }
}
