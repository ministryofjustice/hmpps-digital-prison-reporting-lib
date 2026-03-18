package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.FormulaEngine

data class CoreDownloadContext(
  override val schemaFields: List<SchemaField>,
  override val reportFields: List<ReportField>? = null,
  override val validatedFilters: List<ConfiguredApiRepository.Filter>,
  override val formulaEngine: FormulaEngine,
  override val sortedAsc: Boolean,
  override val sortColumn: String? = null,
  override val selectedAndValidatedColumns: Set<String>? = null,
) : DownloadContext
