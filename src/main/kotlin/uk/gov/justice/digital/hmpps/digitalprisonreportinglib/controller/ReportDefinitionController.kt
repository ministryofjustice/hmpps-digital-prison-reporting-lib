package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ReportDefinitionService

@Validated
@RestController
@Tag(name = "Report Definition API")
class ReportDefinitionController(val reportDefinitionService: ReportDefinitionService) {

  companion object {
    const val DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION = """This optional parameter sets the path of the directory of the data product definition files your application will use.
      "This query parameter is intended to be used in conjunction with the `dpr.lib.dataProductDefinitions.host` property to retrieve definition files from another application by using a web client."""
    const val DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE = "definitions/prisons/orphanage"
  }

  @GetMapping("/definitions")
  @Operation(
    description = "Gets summaries of all report definitions",
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
      description = DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): List<ReportDefinitionSummary> {
    return reportDefinitionService.getListForUser(
      renderMethod,
      if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
      dataProductDefinitionsPath,
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
      description = DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): SingleVariantReportDefinition {
    return reportDefinitionService.getDefinition(
      reportId,
      variantId,
      if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
      dataProductDefinitionsPath,
    )
  }
}
