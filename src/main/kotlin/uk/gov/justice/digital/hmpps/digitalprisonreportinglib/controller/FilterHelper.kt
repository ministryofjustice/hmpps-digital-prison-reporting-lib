package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import org.springframework.stereotype.Component

@Component
class FilterHelper {
  fun filtersOnly(filters: Map<String, String>): Map<String, String> {
    return filters.entries
      .filter { it.key.startsWith(DataApiSyncController.FiltersPrefix.FILTERS_PREFIX) }
      .filter { it.value.isNotBlank() }
      .associate { (k, v) -> k.removePrefix(DataApiSyncController.FiltersPrefix.FILTERS_PREFIX) to v }
  }
}
