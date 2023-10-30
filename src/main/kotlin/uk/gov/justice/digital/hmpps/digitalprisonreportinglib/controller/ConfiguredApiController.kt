package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_QUERY_DESCRIPTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.FILTERS_QUERY_EXAMPLE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService

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
  ): List<Map<String, Any>> {
    return configuredApiService.validateAndFetchData(
      reportId,
      reportVariantId,
      filtersOnly(filters),
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      authentication.caseloads,
    )
  }

  @GetMapping("/reports/{reportId}/{reportVariantId}/count")
  @Operation(
    description = "Returns the number of records for the given report ID and report variant ID filtered by the filters provided in the query.",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
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
  ): Count {
    return configuredApiService.validateAndCount(reportId, reportVariantId, filtersOnly(filters), authentication.caseloads)
  }

  private fun filtersOnly(filters: Map<String, String>): Map<String, String> {
    return filters.entries
      .filter { it.key.startsWith(FILTERS_PREFIX) }
      .filter { it.value.isNotBlank() }
      .associate { (k, v) -> k.removePrefix(FILTERS_PREFIX) to v }
  }
}
