package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Service
class ReportDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val mapper: ReportDefinitionMapper,
) {

  fun getListForUser(renderMethod: RenderMethod?, maxStaticOptions: Long, userToken: DprAuthAwareAuthenticationToken?): List<ReportDefinition> {
    return productDefinitionRepository.getProductDefinitions()
      .map { mapper.map(it, renderMethod, maxStaticOptions, userToken) }
      .filter { it.variants.isNotEmpty() }
  }

  fun getDefinition(reportId: String, variantId: String, maxStaticOptions: Long, userToken: DprAuthAwareAuthenticationToken?): SingleVariantReportDefinition {
    return mapper.map(
      productDefinitionRepository.getSingleReportProductDefinition(reportId, variantId),
      maxStaticOptions,
      userToken,
    )
  }
}
