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
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_QUERY_DESCRIPTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_QUERY_EXAMPLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ResponseHeader
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.AsyncDataApiService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.CsvStreamingSupport
import java.util.Collections.singletonList

@Validated
@RestController
@Tag(name = "Data API - Asynchronous")
@ConditionalOnProperty("dpr.lib.aws.sts.enabled", havingValue = "true")
class DataApiAsyncController(
  val asyncDataApiService: AsyncDataApiService,
  val filterHelper: FilterHelper,
  val csvStreamingSupport: CsvStreamingSupport,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/async/reports/{reportId}/{reportVariantId}")
  @Operation(
    description = "Executes asynchronously the dataset query for the given report and stores the result into an external table." +
      "The response returned contains the table ID and the execution ID. " +
      "This is the asynchronous version of the /reports/{reportId}/{reportVariantId} API.",
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
  fun asyncConfiguredApiExecuteQuery(
    @RequestParam sortColumn: String?,
    @RequestParam sortedAsc: Boolean?,
    @Parameter(
      description = "$FILTERS_QUERY_DESCRIPTION Note: For legacy nomis and bodmis reports, for filters deriving from DPD parameters(prompts)," +
        "there is no need for these to be suffixed with .start and .end. For example, filters.start_date and filters.end_date are perfectly valid in this case.",
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
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementExecutionResponse> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.validateAndExecuteStatementAsync(
          reportId = reportId,
          reportVariantId = reportVariantId,
          filters = filterHelper.filtersOnly(filters),
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
      .body(null)
  }

  @GetMapping("/async/dashboards/{reportId}/{dashboardId}")
  @Operation(
    description = "Executes asynchronously the dataset query for the given dashboard and stores the result into an external table." +
      "The response returned contains the table ID and the execution ID. ",
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
  fun asyncExecuteDashboard(
    @PathVariable("reportId") reportId: String,
    @PathVariable("dashboardId") dashboardId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    authentication: Authentication,
  ): ResponseEntity<StatementExecutionResponse> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.validateAndExecuteStatementAsync(
          reportId = reportId,
          dashboardId = dashboardId,
          userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
          dataProductDefinitionsPath = dataProductDefinitionsPath,
          filters = filterHelper.filtersOnly(filters),
        ),
      )
  } catch (exception: NoDataAvailableException) {
    val headers = HttpHeaders()
    headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

    ResponseEntity
      .status(HttpStatus.OK)
      .headers(headers)
      .body(null)
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/statements/{statementId}/status")
  @Operation(
    description = "Returns the status of the statement execution based on the statement ID provided." +
      "The following status values can be returned: \n" +
      "ABORTED - The query run was stopped by the user.\n" +
      "ALL - A status value that includes all query statuses. This value can be used to filter results.\n" +
      "FAILED - The query run failed.\n" +
      "FINISHED - The query has finished running.\n" +
      "PICKED - The query has been chosen to be run.\n" +
      "STARTED - The query run has started.\n" +
      "SUBMITTED - The query was submitted, but not yet processed.\n" +
      "Note: When the status is FAILED the error field of the response will be populated." +
      "ResultRows is the number of rows returned from the SQL statement. A -1 indicates the value is null." +
      "ResultSize is the size in bytes of the returned results. A -1 indicates the value is null.\n" +
      "For Athena: \n" +
      "Athena automatically retries your queries in cases of certain transient errors. " +
      "As a result, you may see the query state transition from STARTED or FAILED to SUBMITTED.\n",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getQueryExecutionStatus(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @PathVariable("statementId") statementId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    @Parameter(
      description = "External table ID.",
      example = "reports._6b3c6dfb_f601_4795_8ee5_2ad65b7fb283",
    )
    @RequestParam(
      "tableId",
      required = false,
    )
    tableId: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementExecutionStatus> = ResponseEntity
    .status(HttpStatus.OK)
    .body(
      asyncDataApiService.getStatementStatus(
        statementId = statementId,
        reportId = reportId,
        reportVariantId = reportVariantId,
        userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
        dataProductDefinitionsPath,
        tableId,
      ),
    )

  @GetMapping("/reports/{reportId}/dashboards/{dashboardId}/statements/{statementId}/status")
  @Operation(
    description = "Returns the status of the dashboard statement execution based on the statement ID provided." +
      "The following status values can be returned: \n" +
      "ABORTED - The query run was stopped by the user.\n" +
      "ALL - A status value that includes all query statuses. This value can be used to filter results.\n" +
      "FAILED - The query run failed.\n" +
      "FINISHED - The query has finished running.\n" +
      "PICKED - The query has been chosen to be run.\n" +
      "STARTED - The query run has started.\n" +
      "SUBMITTED - The query was submitted, but not yet processed.\n" +
      "Note: When the status is FAILED the error field of the response will be populated." +
      "ResultRows is the number of rows returned from the SQL statement. A -1 indicates the value is null." +
      "ResultSize is the size in bytes of the returned results. A -1 indicates the value is null.\n" +
      "For Athena: \n" +
      "Athena automatically retries your queries in cases of certain transient errors. " +
      "As a result, you may see the query state transition from STARTED or FAILED to SUBMITTED.\n",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getDashboardExecutionStatus(
    @PathVariable("reportId") reportId: String,
    @PathVariable("dashboardId") dashboardId: String,
    @PathVariable("statementId") statementId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    @Parameter(
      description = "External table ID.",
      example = "reports._6b3c6dfb_f601_4795_8ee5_2ad65b7fb283",
    )
    @RequestParam(
      "tableId",
      required = false,
    )
    tableId: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementExecutionStatus> = ResponseEntity
    .status(HttpStatus.OK)
    .body(
      asyncDataApiService.getDashboardStatementStatus(
        statementId = statementId,
        productDefinitionId = reportId,
        dashboardId = dashboardId,
        userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
        dataProductDefinitionsPath,
        tableId,
      ),
    )

  @DeleteMapping("/reports/{reportId}/{reportVariantId}/statements/{statementId}")
  @Operation(
    description = "Cancels the execution of a running query.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun cancelReportQueryExecution(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @PathVariable("statementId") statementId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementCancellationResponse> = ResponseEntity
    .status(HttpStatus.OK)
    .body(
      asyncDataApiService.cancelStatementExecution(
        statementId,
        reportId,
        reportVariantId,
        userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
        dataProductDefinitionsPath,
      ),
    )

  @DeleteMapping("/reports/{reportId}/dashboards/{dashboardId}/statements/{statementId}")
  @Operation(
    description = "Cancels the execution of a running query.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun cancelDashboardQueryExecution(
    @PathVariable("reportId") definitionId: String,
    @PathVariable("dashboardId") dashboardId: String,
    @PathVariable("statementId") statementId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementCancellationResponse> = ResponseEntity
    .status(HttpStatus.OK)
    .body(
      asyncDataApiService.cancelDashboardStatementExecution(
        statementId,
        definitionId,
        dashboardId,
        userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
        dataProductDefinitionsPath,
      ),
    )

  @GetMapping("/report/tables/{tableId}/count")
  @Operation(
    description = "Returns the number of rows of the table which contains the result of a previously executed query.",
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
  fun getExternalTableRowCount(
    @PathVariable("tableId") tableId: String,
    authentication: Authentication,
  ): ResponseEntity<Count> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.count(tableId),
      )
  } catch (exception: NoDataAvailableException) {
    val headers = HttpHeaders()
    headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

    ResponseEntity
      .status(HttpStatus.OK)
      .headers(headers)
      .body(null)
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/tables/{tableId}/count")
  @Operation(
    description = "Returns the number of rows of the table which contains the result of a previously executed query. " +
      "Allows filtering and it is aimed at supporting the interactive journey.",
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
  fun getInteractiveExternalTableRowCount(
    @PathVariable("tableId") tableId: String,
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @RequestParam
    filters: Map<String, String>,
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<Count> = try {
    ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.count(
          tableId,
          reportId,
          reportVariantId,
          filterHelper.filtersOnly(filters),
          userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
          dataProductDefinitionsPath,
        ),
      )
  } catch (exception: NoDataAvailableException) {
    val headers = HttpHeaders()
    headers[ResponseHeader.NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

    ResponseEntity
      .status(HttpStatus.OK)
      .headers(headers)
      .body(null)
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/tables/{tableId}/result")
  @Operation(
    description = "Returns the resulting rows of the executed statement in a paginated " +
      "fashion which has been stored in a dedicated table.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getQueryExecutionResult(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    @PathVariable("tableId") tableId: String,
    @RequestParam(defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    @RequestParam(defaultValue = "10")
    @Min(1)
    pageSize: Long,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    @RequestParam sortColumn: String?,
    @RequestParam sortedAsc: Boolean?,
    authentication: Authentication,
  ): ResponseEntity<List<Map<String, Any?>>> = ResponseEntity
    .status(HttpStatus.OK)
    .body(
      asyncDataApiService.getStatementResult(
        tableId = tableId,
        reportId = reportId,
        reportVariantId = reportVariantId,
        dataProductDefinitionsPath = dataProductDefinitionsPath,
        selectedPage = selectedPage,
        pageSize = pageSize,
        filters = filterHelper.filtersOnly(filters),
        sortedAsc = sortedAsc,
        sortColumn = sortColumn,
        userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
      ),
    )

  @GetMapping("/reports/{reportId}/dashboards/{dashboardId}/tables/{tableId}/result")
  @Operation(
    description = "Returns the resulting rows of the executed statement in a paginated " +
      "fashion which has been stored in a dedicated table.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getDashboardQueryExecutionResult(
    @PathVariable("reportId") reportId: String,
    @PathVariable("dashboardId") dashboardId: String,
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    @PathVariable("tableId") tableId: String,
    @RequestParam(defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    pageSize: Long? = null,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    authentication: Authentication,
  ): ResponseEntity<List<List<Map<String, Any?>>>> = ResponseEntity
    .status(HttpStatus.OK)
    .body(
      asyncDataApiService.getDashboardStatementResult(
        tableId = tableId,
        reportId = reportId,
        dashboardId = dashboardId,
        dataProductDefinitionsPath = dataProductDefinitionsPath,
        selectedPage = selectedPage,
        pageSize = pageSize,
        filters = filterHelper.filtersOnly(filters),
        userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
      ),
    )

  @GetMapping("/reports/{reportId}/{reportVariantId}/tables/{tableId}/result/summary/{summaryId}")
  @Operation(
    description = "Returns a summary of a request, which has been stored in a dedicated table.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getSummaryQueryExecutionResult(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    @PathVariable("tableId") tableId: String,
    @PathVariable("summaryId") summaryId: String,
    @Parameter(
      description = FILTERS_QUERY_DESCRIPTION,
      example = FILTERS_QUERY_EXAMPLE,
    )
    @RequestParam
    filters: Map<String, String>,
    authentication: Authentication,
  ): ResponseEntity<List<Map<String, Any?>>> {
    val summaryResult = asyncDataApiService.getSummaryResult(
      tableId = tableId,
      summaryId = summaryId,
      reportId = reportId,
      reportVariantId = reportVariantId,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
      filters = filterHelper.filtersOnly(filters),
      userToken = authentication as? DprAuthAwareAuthenticationToken,
    )
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(summaryResult)
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/tables/{tableId}/download", produces = ["text/csv"])
  @Operation(
    description = "Streams the entire result set of the async query execution as a csv file.",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun downloadCsv(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @RequestParam(
      "dataProductDefinitionsPath",
      defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    dataProductDefinitionsPath: String? = null,
    @PathVariable("tableId") tableId: String,
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
    authentication: Authentication,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    val downloadContext = asyncDataApiService.prepareAsyncDownloadContext(
      reportId = reportId,
      reportVariantId = reportVariantId,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
      filters = filterHelper.filtersOnly(filters),
      selectedColumns = columns,
      sortedAsc = sortedAsc,
      sortColumn = sortColumn,
      userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
    )

    csvStreamingSupport.streamCsv(
      reportId,
      reportVariantId,
      request,
      response,
    ) { writer ->
      asyncDataApiService.downloadCsv(
        writer = writer,
        tableId = tableId,
        asyncDownloadContext = downloadContext,
      )
    }
  }
}
