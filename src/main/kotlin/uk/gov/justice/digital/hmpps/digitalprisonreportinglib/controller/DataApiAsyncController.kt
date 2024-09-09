package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
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
import java.util.Collections.singletonList

@Validated
@RestController
@Tag(name = "Data API - Asynchronous")
class DataApiAsyncController(val asyncDataApiService: AsyncDataApiService, val filterHelper: FilterHelper) {

  @GetMapping("/async/reports/{reportId}/{reportVariantId}")
  @Operation(
    description = "Executes asynchronously the dataset query for the given report and stores the result into an external table." +
      "The response returned contains the table ID and the execution ID. " +
      "This is the asynchronous version of the /reports/{reportId}/{reportVariantId} API.",
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
  fun asyncConfiguredApiExecuteQuery(
    @RequestParam sortColumn: String?,
    @RequestParam(defaultValue = "false") sortedAsc: Boolean,
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
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementExecutionResponse> {
    return try {
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
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun getQueryExecutionStatus(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @PathVariable("statementId") statementId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementExecutionStatus> {
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.getStatementStatus(statementId, reportId, reportVariantId, dataProductDefinitionsPath),
      )
  }

  @DeleteMapping("/reports/{reportId}/{reportVariantId}/statements/{statementId}")
  @Operation(
    description = "Cancels the execution of a running query.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun cancelQueryExecution(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @PathVariable("statementId") statementId: String,
    @Parameter(
      description = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_DESCRIPTION,
      example = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE,
    )
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    authentication: Authentication,
  ): ResponseEntity<StatementCancellationResponse> {
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.cancelStatementExecution(statementId, reportId, reportVariantId, dataProductDefinitionsPath),
      )
  }

  @GetMapping("/report/tables/{tableId}/count")
  @Operation(
    description = "Returns the number of rows of the table which contains the result of a previously executed query.",
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
  fun getExternalTableRowCount(
    @PathVariable("tableId") tableId: String,
    authentication: Authentication,
  ): ResponseEntity<Count> {
    return try {
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
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/tables/{tableId}/result")
  @Operation(
    description = "Returns the resulting rows of the executed statement in a paginated " +
      "fashion which has been stored in a dedicated table.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun getQueryExecutionResult(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    @PathVariable("tableId") tableId: String,
    @RequestParam(defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    @RequestParam(defaultValue = "10")
    @Min(1)
    pageSize: Long,
    authentication: Authentication,
  ): ResponseEntity<List<Map<String, Any?>>> {
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.getStatementResult(
          tableId,
          reportId,
          reportVariantId,
          dataProductDefinitionsPath,
          selectedPage,
          pageSize,
        ),
      )
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/tables/{tableId}/result/summary/{summaryId}")
  @Operation(
    description = "Returns a summary of a request, which has been stored in a dedicated table.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun getSummaryQueryExecutionResult(
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @RequestParam("dataProductDefinitionsPath", defaultValue = ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE)
    dataProductDefinitionsPath: String? = null,
    @PathVariable("tableId") tableId: String,
    @PathVariable("summaryId") summaryId: String,
    authentication: Authentication,
  ): ResponseEntity<List<Map<String, Any?>>> {
    return ResponseEntity
      .status(HttpStatus.OK)
      .body(
        asyncDataApiService.getSummaryResult(
          tableId,
          summaryId,
          reportId,
          reportVariantId,
          dataProductDefinitionsPath,
        ),
      )
  }
}
