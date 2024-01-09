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

  fun getListForUser(
    renderMethod: RenderMethod?,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): List<ReportDefinition> {
    return productDefinitionRepository.getProductDefinitions(dataProductDefinitionsPath)
      .map { mapper.map(it, renderMethod, maxStaticOptions, userToken, dataProductDefinitionsPath) }
      .filter { it.variants.isNotEmpty() }
  }

  fun getDefinition(
    reportId: String,
    variantId: String,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): SingleVariantReportDefinition {
    return mapper.map(
      definition = productDefinitionRepository.getSingleReportProductDefinition(reportId, variantId, dataProductDefinitionsPath),
      maxStaticOptions = maxStaticOptions,
      userToken = userToken,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
    )
  }
}
