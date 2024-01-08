package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ReportDefinitionService

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
    @Parameter(
      description = "This optional parameter sets the maximum number of static options returned when there is a dynamic filter and returnAsStaticOptions is true.",
      example = "30",
    )
    @RequestParam("maxStaticOptions", defaultValue = "20")
    @Min(1)
    maxStaticOptions: Long,
    @RequestParam("dataProductDefinitionsPath")
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): List<ReportDefinition> {
    return reportDefinitionService.getListForUser(
      renderMethod,
      maxStaticOptions,
      if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
      dataProductDefinitionsPath
    )
  }

  @GetMapping("/definitions/{reportId}/{variantId}")
  @Operation(
    description = "Gets report definition containing a single variant.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun definition(
    @Parameter(
      description = "The ID of the report definition.",
      example = "external-movements",
    )
    @PathVariable("reportId")
    reportId: String,
    @Parameter(
      description = "The ID of the variant definition.",
      example = "list",
    )
    @PathVariable("variantId")
    variantId: String,
    @Parameter(
      description = "This optional parameter sets the maximum number of static options returned when there is a dynamic filter and returnAsStaticOptions is true.",
      example = "30",
    )
    @RequestParam("maxStaticOptions", defaultValue = "20")
    @Min(1)
    maxStaticOptions: Long,
    @RequestParam("dataProductDefinitionsPath")
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): SingleVariantReportDefinition {
    return reportDefinitionService.getDefinition(
      reportId,
      variantId,
      maxStaticOptions,
      if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
      dataProductDefinitionsPath
    )
  }
}
