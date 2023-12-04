package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository

@Service
class ReportDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val mapper: ReportDefinitionMapper,
) {

  fun getListForUser(renderMethod: RenderMethod?, caseLoads: List<String>): List<ReportDefinition> {
    return productDefinitionRepository.getProductDefinitions()
      .map { mapper.map(it, renderMethod, caseLoads) }
      .filter { it.variants.isNotEmpty() }
  }

  fun getDefinition(reportId: String, variantId: String, caseLoads: List<String>): SingleVariantReportDefinition {
    return mapper.map(productDefinitionRepository.getSingleReportProductDefinition(reportId, variantId), caseLoads)
  }
}
