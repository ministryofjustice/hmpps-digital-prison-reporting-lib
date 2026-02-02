package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.FormulaEngine

data class DownloadContext(
  val schemaFields: List<SchemaField>,
  val reportFields: List<ReportField>? = null,
  val validatedFilters: List<ConfiguredApiRepository.Filter>,
  val formulaEngine: FormulaEngine,
  val sortedAsc: Boolean,
  val sortColumn: String? = null,
  val selectedAndValidatedColumns: Set<String>? = null,
)
