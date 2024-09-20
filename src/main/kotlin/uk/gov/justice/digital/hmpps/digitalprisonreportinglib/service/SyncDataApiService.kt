package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Service
class SyncDataApiService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepository,
  @Value("\${URL_ENV_SUFFIX:#{null}}") val env: String? = null,
) : CommonDataApiService() {

  companion object {
    const val INVALID_REPORT_ID_MESSAGE = "Invalid report id provided:"
    const val INVALID_REPORT_VARIANT_ID_MESSAGE = "Invalid report variant id provided:"
    const val INVALID_FILTERS_MESSAGE = "Invalid filters provided."
    const val INVALID_STATIC_OPTIONS_MESSAGE = "Invalid static options provided."
    const val INVALID_DYNAMIC_OPTIONS_MESSAGE = "Invalid dynamic options length provided."
    const val INVALID_DYNAMIC_FILTER_MESSAGE = "Error. This filter is not a dynamic filter."
    const val MISSING_MANDATORY_FILTER_MESSAGE = "Mandatory filter value not provided:"
    const val FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE = "Filter value does not match pattern:"
    const val SCHEMA_REF_PREFIX = "\$ref:"
  }

  fun validateAndFetchData(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    userToken: DprAuthAwareAuthenticationToken?,
    reportFieldId: Set<String>? = null,
    prefix: String? = null,
    dataProductDefinitionsPath: String? = null,
    datasetForFilter: Dataset? = null,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    val dynamicFilter = buildAndValidateDynamicFilter(reportFieldId?.first(), prefix, productDefinition)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    val formulaEngine = FormulaEngine(productDefinition.report.specification?.field ?: emptyList(), env)
    return configuredApiRepository
      .executeQuery(
        query = datasetForFilter?.query ?: productDefinition.reportDataset.query,
        filters = validateAndMapFilters(productDefinition, filters, reportFieldId) + dynamicFilter,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = datasetForFilter?.let { findSortColumn(sortColumn, it) } ?: sortColumnFromQueryOrGetDefault(productDefinition, sortColumn),
        sortedAsc = sortedAsc,
        policyEngineResult = datasetForFilter?.let { Policy.PolicyResult.POLICY_PERMIT } ?: policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        dataSourceName = productDefinition.datasource.name,
        reportFilter = productDefinition.report.filter,
      )
      .let { records ->
        applyFormulasSelectivelyAndFormatColumns(
          records,
          productDefinition,
          formulaEngine,
          datasetForFilter,
        )
      }
  }

  fun validateAndCount(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): Count {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(
      reportId,
      reportVariantId,
      dataProductDefinitionsPath,
    )
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    return Count(
      configuredApiRepository.count(
        filters = validateAndMapFilters(productDefinition, filters),
        query = productDefinition.reportDataset.query,
        reportId = reportId,
        policyEngineResult = policyEngine.execute(),
        dataSourceName = productDefinition.datasource.name,
        productDefinition = productDefinition,
      ),
    )
  }

  private fun applyFormulasSelectivelyAndFormatColumns(
    records: List<Map<String, Any?>>,
    productDefinition: SingleReportProductDefinition,
    formulaEngine: FormulaEngine,
    datasetForFilter: Dataset?,
  ) = datasetForFilter?.let {
    records.map { row ->
      formatColumnNamesToSourceFieldNamesCasing(row, datasetForFilter.schema.field.map(SchemaField::name))
    }
  } ?: formatColumnsAndApplyFormulas(records, productDefinition.reportDataset.schema.field, formulaEngine)

  private fun formatColumnsAndApplyFormulas(
    records: List<Map<String, Any?>>,
    schemaFields: List<SchemaField>,
    formulaEngine: FormulaEngine,
  ) = records
    .map { row -> formatColumnNamesToSourceFieldNamesCasing(row, schemaFields.map(SchemaField::name)) }
    .map(formulaEngine::applyFormulas)
}
