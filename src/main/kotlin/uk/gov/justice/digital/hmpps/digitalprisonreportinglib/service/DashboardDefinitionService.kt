package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository

@Service
class DashboardDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val dashboardDefinitionMapper: DashboardDefinitionMapper,
) {
  fun getDashboardDefinition(
    dataProductDefinitionId: String,
    dashboardId: String,
    dataProductDefinitionsPath: String? = null,
  ): DashboardDefinition {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(
      dataProductDefinitionId,
      dashboardId,
      dataProductDefinitionsPath,
    )

    return dashboardDefinitionMapper.toDashboardDefinition(productDefinition.dashboard, productDefinition.allDatasets)
  }
}
