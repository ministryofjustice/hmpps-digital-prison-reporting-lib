package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.MetricsDataService

@Validated
@RestController
@Tag(name = "Metrics Data API")
class MetricsDataController(val metricsDataService: MetricsDataService) {

//  @GetMapping("/reports/{reportId}/metrics/{metricId}")
//  @Operation(
//    description = "Returns the metric dataset for the given data product definition ID.",
//    security = [ SecurityRequirement(name = "bearer-jwt") ],
//    responses = [
//      ApiResponse(
//        headers = [
//          Header(
//            name = ResponseHeader.NO_DATA_WARNING_HEADER_NAME,
//            description = "Provides additional information about why no data has been returned.",
//          ),
//        ],
//      ),
//    ],
//  )
//  fun configuredApiDataset(
//    @PathVariable("reportId") reportId: String,
//    @PathVariable("metricId") metricId: String,
//    @Parameter(
//      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
//      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
//    )
//    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
//    dataProductDefinitionsPath: String? = null,
//    authentication: Authentication,
//  ): ResponseEntity<MetricDataResponse> {
//    return try {
//      ResponseEntity
//        .status(HttpStatus.OK)
//        .body(
//          MetricDataResponse(
//            id = metricId,
//            data = metricsDataService.validateAndFetchData(
//              dataProductDefinitionId = reportId,
//              metricId = metricId,
//              userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
//              dataProductDefinitionsPath = dataProductDefinitionsPath,
//            ),
//            updated = LocalDateTime.now(),
//          ),
//        )
//    } catch (exception: NoDataAvailableException) {
//      val headers = HttpHeaders()
//      headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = Collections.singletonList(exception.reason)
//
//      ResponseEntity
//        .status(HttpStatus.OK)
//        .headers(headers)
//        .body(null)
//    }
//  }
}
