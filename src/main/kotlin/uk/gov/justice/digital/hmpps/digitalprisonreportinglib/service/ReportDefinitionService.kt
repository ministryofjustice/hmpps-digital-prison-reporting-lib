package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Service
class ReportDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val mapper: ReportDefinitionMapper,
  val summaryMapper: ReportDefinitionSummaryMapper,
) {

  fun getListForUser(
    renderMethod: RenderMethod?,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): List<ReportDefinitionSummary> {
    return productDefinitionRepository.getProductDefinitions(dataProductDefinitionsPath)
      .map { summaryMapper.map(it, renderMethod) }
      // We might want to remove this filtering if we want to allow an empty list of variants for DPDs with metrics only
      .filter { it.variants.isNotEmpty() }
  }

  fun getDefinition(
    reportId: String,
    variantId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): SingleVariantReportDefinition {
    return mapper.map(
      definition = productDefinitionRepository.getSingleReportProductDefinition(reportId, variantId, dataProductDefinitionsPath),
      userToken = userToken,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
    )
  }
}
