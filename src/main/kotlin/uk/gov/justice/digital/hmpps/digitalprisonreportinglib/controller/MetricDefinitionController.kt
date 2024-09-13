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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ReportDefinitionController.Companion.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ReportDefinitionController.Companion.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.MetricDefinitionService

@Validated
@RestController
@Tag(name = "Metric Definition API")
class MetricDefinitionController(val metricDefinitionService: MetricDefinitionService) {

  @GetMapping("/definitions/{dataProductDefinitionId}/dashboards/{dashboardId}")
  @Operation(
    description = "Gets the metric dashboard definition.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun dashboardDefinition(
    @Parameter(
      description = "The ID of the Data Product Definition.",
      example = "external-movements",
    )
    @PathVariable("dataProductDefinitionId")
    dataProductDefinitionId: String,
    @Parameter(
      description = "The ID of the dashboard.",
      example = "dashboardId",
    )
    @PathVariable("dashboardId")
    dashboardId: String,
    @Parameter(
      description = DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): DashboardDefinition {
    return metricDefinitionService.getDashboardDefinition(
      dataProductDefinitionId,
      dashboardId,
      dataProductDefinitionsPath,
    )
  }

  @GetMapping("/definitions/{dataProductDefinitionId}/metrics/{metricId}")
  @Operation(
    description = "Gets the metric definition.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun metricDefinition(
    @Parameter(
      description = "The ID of the Data Product Definition.",
      example = "external-movements",
    )
    @PathVariable("dataProductDefinitionId")
    dataProductDefinitionId: String,
    @Parameter(
      description = "The ID of the metric.",
      example = "metricId",
    )
    @PathVariable("metricId")
    metricId: String,
    @Parameter(
      description = DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): MetricDefinition {
    return metricDefinitionService.getMetricDefinition(
      dataProductDefinitionId,
      metricId,
      dataProductDefinitionsPath,
    )
  }
}
