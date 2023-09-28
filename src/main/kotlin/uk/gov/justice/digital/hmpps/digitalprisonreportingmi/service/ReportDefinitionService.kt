package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ProductDefinitionRepository

@Service
class ReportDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val mapper: ReportDefinitionMapper,
) {

  fun getListForUser(renderMethod: RenderMethod?): List<ReportDefinition> {
    return productDefinitionRepository.getProductDefinitions()
      .map { mapper.map(it, renderMethod) }
      .filter { it.variants.isNotEmpty() }
  }
}
