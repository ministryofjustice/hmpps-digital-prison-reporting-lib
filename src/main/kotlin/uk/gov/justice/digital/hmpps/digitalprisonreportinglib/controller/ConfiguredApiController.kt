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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_QUERY_DESCRIPTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_QUERY_EXAMPLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService
import java.util.Collections.singletonList

const val NO_DATA_WARNING_HEADER_NAME = "x-no-data-warning"

@Validated
@RestController
@Tag(name = "Configured Data API")
class ConfiguredApiController(val configuredApiService: ConfiguredApiService) {
  object FiltersPrefix {
    const val FILTERS_PREFIX = "filters."
    const val RANGE_FILTER_START_SUFFIX = ".start"
    const val RANGE_FILTER_END_SUFFIX = ".end"
    const val FILTERS_QUERY_DESCRIPTION = """The filter query parameters have to start with the prefix "$FILTERS_PREFIX" followed by the name of the filter.
      |For range filters, like date for instance, these need to be followed by a $RANGE_FILTER_START_SUFFIX or $RANGE_FILTER_END_SUFFIX suffix accordingly.
    """
    const val FILTERS_QUERY_EXAMPLE = """{
        "filters.date$RANGE_FILTER_START_SUFFIX": "2023-04-25",
        "filters.date$RANGE_FILTER_END_SUFFIX": "2023-05-30"
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
            name = NO_DATA_WARNING_HEADER_NAME,
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
    @RequestParam(defaultValue = "false") sortedAsc: Boolean,
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
  ): ResponseEntity<List<Map<String, Any?>>> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
          configuredApiService.validateAndFetchData(
            reportId = reportId,
            reportVariantId = reportVariantId,
            filters = filtersOnly(filters),
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
      headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

      ResponseEntity
        .status(HttpStatus.OK)
        .headers(headers)
        .body(emptyList())
    }
  }

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
            name = NO_DATA_WARNING_HEADER_NAME,
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
  ): ResponseEntity<StatementExecutionResponse> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
          configuredApiService.validateAndExecuteStatementAsync(
            reportId = reportId,
            reportVariantId = reportVariantId,
            filters = filtersOnly(filters),
            sortColumn = sortColumn,
            sortedAsc = sortedAsc,
            userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
            dataProductDefinitionsPath = dataProductDefinitionsPath,
          ),
        )
    } catch (exception: NoDataAvailableException) {
      val headers = HttpHeaders()
      headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

      ResponseEntity
        .status(HttpStatus.OK)
        .headers(headers)
        .body(null)
    }
  }

  @GetMapping("/report/statements/{statementId}/status")
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
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = NO_DATA_WARNING_HEADER_NAME,
            description = "Provides additional information about why no data has been returned.",
          ),
        ],
      ),
    ],
  )
  fun getQueryExecutionStatus(
    @PathVariable("statementId") statementId: String,
    authentication: Authentication,
  ): ResponseEntity<StatementExecutionStatus> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
          configuredApiService.getStatementStatus(statementId),
        )
    } catch (exception: NoDataAvailableException) {
      val headers = HttpHeaders()
      headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

      ResponseEntity
        .status(HttpStatus.OK)
        .headers(headers)
        .body(null)
    }
  }

  @GetMapping("/report/tables/{tableId}/count")
  @Operation(
    description = "Returns the number of rows of the table which contains the result of a previously executed query.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = NO_DATA_WARNING_HEADER_NAME,
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
          configuredApiService.count(tableId),
        )
    } catch (exception: NoDataAvailableException) {
      val headers = HttpHeaders()
      headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

      ResponseEntity
        .status(HttpStatus.OK)
        .headers(headers)
        .body(null)
    }
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/tables/{tableId}/result")
  @Operation(
    description = "Returns the resulting rows of the executed statement in a paginated " +
      "fashion which has been have been stored in a dedicated table.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = NO_DATA_WARNING_HEADER_NAME,
            description = "Provides additional information about why no data has been returned.",
          ),
        ],
      ),
    ],
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
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
          configuredApiService.getStatementResult(
            tableId,
            reportId,
            reportVariantId,
            dataProductDefinitionsPath,
            selectedPage,
            pageSize,
          ),
        )
    } catch (exception: NoDataAvailableException) {
      val headers = HttpHeaders()
      headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

      ResponseEntity
        .status(HttpStatus.OK)
        .headers(headers)
        .body(null)
    }
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/{fieldId}")
  @Operation(
    description = "Returns the dataset for the given report ID and report variant ID filtered by the filters provided in the query.",
    security = [SecurityRequirement(name = "bearer-jwt")],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = NO_DATA_WARNING_HEADER_NAME,
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
    @RequestParam(defaultValue = "false") sortedAsc: Boolean,
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
  ): ResponseEntity<List<String>> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
          configuredApiService.validateAndFetchData(
            reportId = reportId,
            reportVariantId = reportVariantId,
            filters = filtersOnly(filters),
            selectedPage = 1,
            pageSize = pageSize,
            sortColumn = fieldId,
            sortedAsc = sortedAsc,
            userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
            reportFieldId = fieldId,
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
      headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

      ResponseEntity
        .status(HttpStatus.OK)
        .headers(headers)
        .body(emptyList())
    }
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/count")
  @Operation(
    description = "Returns the number of records for the given report ID and report variant ID filtered by the filters provided in the query.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
    responses = [
      ApiResponse(
        headers = [
          Header(
            name = NO_DATA_WARNING_HEADER_NAME,
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
  ): ResponseEntity<Count> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
          configuredApiService.validateAndCount(
            reportId = reportId,
            reportVariantId = reportVariantId,
            filters = filtersOnly(filters),
            userToken = if (authentication is DprAuthAwareAuthenticationToken) authentication else null,
            dataProductDefinitionsPath = dataProductDefinitionsPath,
          ),
        )
    } catch (exception: NoDataAvailableException) {
      val headers = HttpHeaders()
      headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

      ResponseEntity
        .status(HttpStatus.OK)
        .headers(headers)
        .body(Count(0))
    }
  }

  private fun filtersOnly(filters: Map<String, String>): Map<String, String> {
    return filters.entries
      .filter { it.key.startsWith(FILTERS_PREFIX) }
      .filter { it.value.isNotBlank() }
      .associate { (k, v) -> k.removePrefix(FILTERS_PREFIX) to v }
  }
}
