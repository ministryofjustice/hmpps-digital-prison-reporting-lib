package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Min
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.auditing.AuditAction
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.auditing.AuditService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.auditing.AuditStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.SortDirection
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.getExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.DataProductReportableInformation
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_QUERY_DESCRIPTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_QUERY_EXAMPLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ResponseHeader
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.CsvRowWriter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.CsvStreamingSupport
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.XlsxStreamingSupport

@Validated
@RestController
@Tag(name = "Data API - Synchronous")
class DataApiSyncController(
  val dataApiSyncService: SyncDataApiService,
  val filterHelper: FilterHelper,
  val csvStreamingSupport: CsvStreamingSupport,
  val xlsxStreamingSupport: XlsxStreamingSupport,
  val manageUsersClient: ManageUsersClient,
  val auditService: AuditService,
  @Value($$"${dpr.lib.hasProbationDatasources}")
  val hasProbationDatasources: Boolean,
) {
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

  @GetMapping("/v2/reports/{reportId}/{reportVariantId}")
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
  fun configuredApiDatasetV2(
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
    httpRequest: HttpServletRequest,
  ): ResponseEntity<ReportExecutionResults> {
    val cleanFilters = filterHelper.filtersOnly(filters)
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        ReportExecutionResults(
          data = dataApiSyncService.validateAndFetchData(
            reportId = reportId,
            reportVariantId = reportVariantId,
            filters = cleanFilters,
            selectedPage = selectedPage,
            pageSize = pageSize,
            sortColumn = sortColumn,
            sortedAsc = sortedAsc,
            executionContext = httpRequest.getExecutionContext(
              manageUsersClient = manageUsersClient,
              hasProbationDatasources = hasProbationDatasources,
              dataProductReportableInformation = DataProductReportableInformation(
                id = reportId,
                variantId = reportVariantId,
              ),
              preFilters = cleanFilters,
              sortColumnName = sortColumn,
              sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
            ),
          ),
        ),
      )
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
    httpRequest: HttpServletRequest,
  ): ResponseEntity<List<Map<String, Any?>>> {
    val cleanFilters = filterHelper.filtersOnly(filters)
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        dataApiSyncService.validateAndFetchData(
          reportId = reportId,
          reportVariantId = reportVariantId,
          filters = cleanFilters,
          selectedPage = selectedPage,
          pageSize = pageSize,
          sortColumn = sortColumn,
          sortedAsc = sortedAsc,
          executionContext = httpRequest.getExecutionContext(
            manageUsersClient = manageUsersClient,
            hasProbationDatasources = hasProbationDatasources,
            dataProductReportableInformation = DataProductReportableInformation(
              id = reportId,
              variantId = reportVariantId,
            ),
            preFilters = cleanFilters,
            sortColumnName = sortColumn,
            sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
          ),
        ),
      )
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
    httpRequest: HttpServletRequest,
  ): ResponseEntity<Count> = ResponseEntity
    .status(HttpStatus.OK)
    .body(
      dataApiSyncService.validateAndCount(
        reportId = reportId,
        reportVariantId = reportVariantId,
        filters = filterHelper.filtersOnly(filters),
        executionContext = httpRequest.getExecutionContext(
          manageUsersClient,
          hasProbationDatasources,
          DataProductReportableInformation(
            id = reportId,
            variantId = reportVariantId,
          ),
        ),
      ),
    )

  @GetMapping("/v2/reports/{reportId}/dashboards/{dashboardId}")
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
  fun configuredApiDatasetForDashboardV2(
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
    httpRequest: HttpServletRequest,
  ): ResponseEntity<DashboardExecutionResults> {
    val cleanFilters = filterHelper.filtersOnly(filters)
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        DashboardExecutionResults(
          data = dataApiSyncService.validateAndFetchDataForDashboard(
            reportId = reportId,
            dashboardId = dashboardId,
            filters = cleanFilters,
            selectedPage = selectedPage,
            pageSize = pageSize,
            sortColumn = sortColumn,
            sortedAsc = sortedAsc,
            executionContext = httpRequest.getExecutionContext(
              manageUsersClient = manageUsersClient,
              hasProbationDatasources = hasProbationDatasources,
              dataProductReportableInformation = DataProductReportableInformation(
                id = reportId,
                variantId = dashboardId,
              ),
              preFilters = cleanFilters,
              sortColumnName = sortColumn,
              sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
            ),
          ),
        ),
      )
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
    httpRequest: HttpServletRequest,
  ): ResponseEntity<List<Map<String, Any?>>> {
    val cleanFilters = filterHelper.filtersOnly(filters)
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        dataApiSyncService.validateAndFetchDataForDashboard(
          reportId = reportId,
          dashboardId = dashboardId,
          filters = cleanFilters,
          selectedPage = selectedPage,
          pageSize = pageSize,
          sortColumn = sortColumn,
          sortedAsc = sortedAsc,
          executionContext = httpRequest.getExecutionContext(
            manageUsersClient = manageUsersClient,
            hasProbationDatasources = hasProbationDatasources,
            dataProductReportableInformation = DataProductReportableInformation(
              id = reportId,
              variantId = dashboardId,
            ),
            preFilters = cleanFilters,
            sortColumnName = sortColumn,
            sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
          ),
        ),
      )
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/download", produces = ["text/csv"])
  @Operation(
    description = "Streams the entire result set of the sync query execution as a csv file.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun downloadCsv(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    @Parameter(
      description = "List of column names to include in the generated report. If not provided all the columns will be returned.",
    )
    @RequestParam(required = false)
    columns: List<String>? = null,
    @RequestParam sortColumn: String?,
    @RequestParam sortedAsc: Boolean?,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    val cleanFilters = filterHelper.filtersOnly(filters)
    val executionContext = request.getExecutionContext(
      manageUsersClient = manageUsersClient,
      hasProbationDatasources = hasProbationDatasources,
      dataProductReportableInformation = DataProductReportableInformation(
        id = reportId,
        variantId = reportVariantId,
      ),
      preFilters = cleanFilters,
      sortColumnName = sortColumn,
      sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
    )
    auditService.publishAttempt(AuditAction.DOWNLOAD, executionContext)
    try {
      val downloadContext = dataApiSyncService.prepareSyncDownloadContext(
        reportId = reportId,
        reportVariantId = reportVariantId,
        filters = cleanFilters,
        selectedColumns = columns,
        sortedAsc = sortedAsc,
        sortColumn = sortColumn,
        executionContext = request.getExecutionContext(
          manageUsersClient = manageUsersClient,
          hasProbationDatasources = hasProbationDatasources,
          dataProductReportableInformation = DataProductReportableInformation(
            id = reportId,
            variantId = reportVariantId,
          ),
          preFilters = cleanFilters,
          sortColumnName = sortColumn,
          sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
        ),
      )

      val stream = csvStreamingSupport.streamCsv(
        reportId,
        reportVariantId,
        request,
        response,
      ) { writer ->
        CsvRowWriter(writer).use { rowWriter ->
          dataApiSyncService.download(
            rowWriter = rowWriter,
            downloadContext = downloadContext,
          )
        }
      }
      auditService.publish(AuditAction.DOWNLOAD, AuditStatus.SUCCESS, executionContext)
      return stream
    } catch (e: Throwable) {
      auditService.publish(AuditAction.DOWNLOAD, AuditStatus.FAILURE, executionContext)
      throw e
    }
  }

  @GetMapping(
    "/reports/{reportId}/{reportVariantId}/download/xlsx",
    produces = [XlsxStreamingSupport.XLSX_CONTENT_TYPE],
  )
  @Operation(
    description = "Streams the entire result set of the sync query execution as an Excel (xlsx) file. " +
      "Unlike the csv download, cell types are explicit, so values such as room numbers are not " +
      "reinterpreted as dates when the file is opened in Excel.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun downloadXlsx(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    @Parameter(
      description = "List of column names to include in the generated report. If not provided all the columns will be returned.",
    )
    @RequestParam(required = false)
    columns: List<String>? = null,
    @RequestParam sortColumn: String?,
    @RequestParam sortedAsc: Boolean?,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    val cleanFilters = filterHelper.filtersOnly(filters)
    val executionContext = request.getExecutionContext(
      manageUsersClient = manageUsersClient,
      hasProbationDatasources = hasProbationDatasources,
      dataProductReportableInformation = DataProductReportableInformation(
        id = reportId,
        variantId = reportVariantId,
      ),
      preFilters = cleanFilters,
      sortColumnName = sortColumn,
      sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
    )
    auditService.publishAttempt(AuditAction.DOWNLOAD, executionContext)
    try {
      val downloadContext = dataApiSyncService.prepareSyncDownloadContext(
        reportId = reportId,
        reportVariantId = reportVariantId,
        filters = filterHelper.filtersOnly(filters),
        selectedColumns = columns,
        sortedAsc = sortedAsc,
        sortColumn = sortColumn,
        executionContext = request.getExecutionContext(
          manageUsersClient = manageUsersClient,
          hasProbationDatasources = hasProbationDatasources,
          dataProductReportableInformation = DataProductReportableInformation(
            id = reportId,
            variantId = reportVariantId,
          ),
          preFilters = cleanFilters,
          sortColumnName = sortColumn,
          sortColumnDirection = sortedAsc?.let { if (it) SortDirection.ASC.name else SortDirection.DESC.name },
        ),
      )

      val stream = xlsxStreamingSupport.streamXlsx(
        reportId,
        reportVariantId,
        response,
      ) { rowWriter ->
        dataApiSyncService.download(
          rowWriter = rowWriter,
          downloadContext = downloadContext,
        )
      }
      auditService.publish(AuditAction.DOWNLOAD, AuditStatus.SUCCESS, executionContext)
      return stream
    } catch (e: Throwable) {
      auditService.publish(AuditAction.DOWNLOAD, AuditStatus.FAILURE, executionContext)
      throw e
    }
  }
}
