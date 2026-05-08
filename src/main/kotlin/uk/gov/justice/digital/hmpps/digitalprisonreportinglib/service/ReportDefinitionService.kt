package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException

@Service
class ReportDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val mapper: ReportDefinitionMapper,
  val summaryMapper: ReportDefinitionSummaryMapper,
  val productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker,
) {

  fun getListForUser(
    renderMethod: RenderMethod?,
    dataProductDefinitionsPath: String? = null,
  ): List<ReportDefinitionSummary> = productDefinitionRepository.getProductDefinitions(dataProductDefinitionsPath)
    .map { summaryMapper.map(it, renderMethod) }
    .filter { containsReportVariantsOrDashboards(it) }

  fun getDefinitionSummary(
    reportId: String,
    dataProductDefinitionsPath: String? = null,
  ): ReportDefinitionSummary = productDefinitionRepository.getProductDefinition(reportId, dataProductDefinitionsPath).let { summaryMapper.map(it, null) }

  fun getDefinition(
    reportId: String,
    variantId: String,
    dataProductDefinitionsPath: String? = null,
    filters: Map<String, String>? = null,
  ): SingleVariantReportDefinition {
    val singleReportDefinitionDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, variantId, dataProductDefinitionsPath)
    checkAuth(singleReportDefinitionDefinition)
    return mapper.mapReport(
      definition = singleReportDefinitionDefinition,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
      filters = filters,
    )
  }

  private fun checkAuth(
    productDefinition: WithPolicy,
  ): Boolean {
    if (!productDefinitionTokenPolicyChecker.determineAuth(productDefinition)) {
      throw UserAuthorisationException("User does not have correct authorisation")
    }
    return true
  }
  private fun containsReportVariantsOrDashboards(it: ReportDefinitionSummary) = it.variants.isNotEmpty() || hasDashboards(it)
  private fun hasDashboards(it: ReportDefinitionSummary) = (it.dashboards?.isNotEmpty() ?: false)
}
