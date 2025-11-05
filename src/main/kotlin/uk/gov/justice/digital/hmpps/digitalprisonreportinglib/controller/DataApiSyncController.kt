package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_QUERY_DESCRIPTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_QUERY_EXAMPLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ResponseHeader
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService
import java.util.Collections.singletonList

@Validated
@RestController
@Tag(name = "Data API - Synchronous")
class DataApiSyncController(val dataApiSyncService: SyncDataApiService, val filterHelper: FilterHelper) {
  object FiltersPrefix {
    const val FILTERS_PREFIX = "filters."
    const val RANGE_FILTER_START_SUFFIX = ".start"
    const val RANGE_FILTER_END_SUFFIX = ".end"
    const val FILTERS_QUERY_DESCRIPTION = """The filter query parameters have to start with the prefix "$FILTERS_PREFIX" followed by the name of the filter.
      For range filters, like date for instance, these need to be followed by a $RANGE_FILTER_START_SUFFIX or $RANGE_FILTER_END_SUFFIX suffix accordingly.
      For multiselect filters, these are passed as one query parameter per filter with a comma separated list of values:
      filters.someMultiselectFilter=a,b,c
    """
    const val FILTERS_QUERY_EXAMPLE = """{
        "filters.date$RANGE_FILTER_START_SUFFIX": "2023-04-25",
        "filters.date$RANGE_FILTER_END_SUFFIX": "2023-05-30",
        "filters.someMultiselectFilter": "a,b,c"
        }"""
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}")
  @Operation(
    description = "Returns the dataset for the given report ID and report variant ID filtered by the filters provided in the query.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = ResponseHeader.NO_DATA_WARNING_HEADER_NAME,
            description = "Provides additional information about why no data has been returned.",
          ),
        ],
      ),
    ],
  )
  fun configuredApiDataset(
    @RequestParam(defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    @RequestParam(defaultValue = "10")
    @Min(1)
    pageSize: Long,
    @RequestParam sortColumn: String?,
    @RequestParam sortedAsc: Boolean?,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<List<Map<String, Any?>>> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        dataApiSyncService.validateAndFetchData(
          reportId = reportId,
          reportVariantId = reportVariantId,
          filters = filterHelper.filtersOnly(filters),
          selectedPage = selectedPage,
          pageSize = pageSize,
          sortColumn = sortColumn,
          sortedAsc = sortedAsc,
          userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
          dataProductDefinitionsPath = dataProductDefinitionsPath,
        ),
      )
  } catch (exception: NoDataAvailableException) {
    val headers = HttpHeaders()
    headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

    ResponseEntity
      .status(HttpStatus.OK)
      .headers(headers)
      .body(emptyList())
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/{fieldId}")
  @Operation(
    description = "Returns the dataset for the given report ID and report variant ID filtered by the filters provided in the query.",
    security = [SecurityRequirement(name = "bearer-jwt")],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = ResponseHeader.NO_DATA_WARNING_HEADER_NAME,
            description = "Provides additional information about why no data has been returned.",
          ),
        ],
      ),
    ],
  )
  fun configuredApiDynamicFilter(
    @RequestParam(defaultValue = "10")
    @Min(1)
    pageSize: Long,
    @RequestParam sortedAsc: Boolean?,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    @Parameter(
      description = "The value to match the start of the fieldId",
      example = "Lond",
    )
    @RequestParam
    prefix: String,
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @Parameter(
      description = "The name of the schema field which will be used as a dynamic filter.",
      example = "name",
    )
    @PathVariable("fieldId")
    @NotNull
    @NotEmpty
    fieldId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<List<String>> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        dataApiSyncService.validateAndFetchData(
          reportId = reportId,
          reportVariantId = reportVariantId,
          filters = filterHelper.filtersOnly(filters),
          selectedPage = 1,
          pageSize = pageSize,
          sortColumn = fieldId,
          sortedAsc = sortedAsc,
          userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
          reportFieldId = setOf(fieldId),
          prefix = prefix,
          dataProductDefinitionsPath = dataProductDefinitionsPath,
        ).asSequence()
          .flatMap {
            it.asSequence()
          }.groupBy({ it.key }, { it.value })
          .values.flatten().map { it.toString() },
      )
  } catch (exception: NoDataAvailableException) {
    val headers = HttpHeaders()
    headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

    ResponseEntity
      .status(HttpStatus.OK)
      .headers(headers)
      .body(emptyList())
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/count")
  @Operation(
    description = "Returns the number of records for the given report ID and report variant ID filtered by the filters provided in the query.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = ResponseHeader.NO_DATA_WARNING_HEADER_NAME,
            description = "Provides additional information about why no data has been returned.",
          ),
        ],
      ),
    ],
  )
  fun configuredApiCount(
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<Count> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        dataApiSyncService.validateAndCount(
          reportId = reportId,
          reportVariantId = reportVariantId,
          filters = filterHelper.filtersOnly(filters),
          userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
          dataProductDefinitionsPath = dataProductDefinitionsPath,
        ),
      )
  } catch (exception: NoDataAvailableException) {
    val headers = HttpHeaders()
    headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

    ResponseEntity
      .status(HttpStatus.OK)
      .headers(headers)
      .body(Count(0))
  }

  @GetMapping("/reports/{reportId}/dashboards/{dashboardId}")
  @Operation(
    description = "Returns the dataset for the given report ID and dashboard ID filtered by the filters provided in the query.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = ResponseHeader.NO_DATA_WARNING_HEADER_NAME,
            description = "Provides additional information about why no data has been returned.",
          ),
        ],
      ),
    ],
  )
  fun configuredApiDatasetForDashboard(
    @RequestParam(defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    @RequestParam(defaultValue = "10")
    @Min(1)
    pageSize: Long,
    @RequestParam sortColumn: String?,
    @RequestParam sortedAsc: Boolean?,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    @PathVariable("reportId") reportId: String,
    @PathVariable("dashboardId") dashboardId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<List<Map<String, Any?>>> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        dataApiSyncService.validateAndFetchDataForDashboard(
          reportId = reportId,
          dashboardId = dashboardId,
          filters = filterHelper.filtersOnly(filters),
          selectedPage = selectedPage,
          pageSize = pageSize,
          sortColumn = sortColumn,
          sortedAsc = sortedAsc,
          userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
          dataProductDefinitionsPath = dataProductDefinitionsPath,
        ),
      )
  } catch (exception: NoDataAvailableException) {
    val headers = HttpHeaders()
    headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

    ResponseEntity
      .status(HttpStatus.OK)
      .headers(headers)
      .body(emptyList())
  }
}
