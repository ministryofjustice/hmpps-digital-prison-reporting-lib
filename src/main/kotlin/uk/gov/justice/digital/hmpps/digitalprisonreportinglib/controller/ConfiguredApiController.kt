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
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_QUERY_DESCRIPTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_QUERY_EXAMPLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthAwareAuthenticationToken
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
    authentication: AuthAwareAuthenticationToken,
  ): ResponseEntity<List<Map<String, Any>>> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
      configuredApiService.validateAndFetchData(
        reportId,
        reportVariantId,
        filtersOnly(filters),
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        authentication,
      ))
    }catch (exception: NoDataAvailableException) {
        val headers = HttpHeaders()
        headers[NO_DATA_WARNING_HEADER_NAME] = singletonList(exception.reason)

        ResponseEntity
          .status(HttpStatus.OK)
          .headers(headers)
          .body(emptyList())
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
    @RequestParam
    prefix: String,
    @PathVariable("reportId") reportId: String,
    @PathVariable("reportVariantId") reportVariantId: String,
    @PathVariable("fieldId")
    @NotNull
    @NotEmpty
    fieldId: String,
    authentication: AuthAwareAuthenticationToken,
  ): ResponseEntity<List<String>> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
          configuredApiService.validateAndFetchData(
            reportId,
            reportVariantId,
            filtersOnly(filters),
            1,
            pageSize,
            fieldId,
            sortedAsc,
            authentication,
            fieldId,
            prefix,
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
    authentication: AuthAwareAuthenticationToken,
  ): ResponseEntity<Count> {
    return try {
      ResponseEntity
        .status(HttpStatus.OK)
        .body(
     configuredApiService.validateAndCount(reportId, reportVariantId, filtersOnly(filters), authentication)
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
