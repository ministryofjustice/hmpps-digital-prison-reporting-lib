package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Service
class DashboardDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val dashboardDefinitionMapper: DashboardDefinitionMapper,
) {
  fun getDashboardDefinition(
    dataProductDefinitionId: String,
    dashboardId: String,
    dataProductDefinitionsPath: String? = null,
    userToken: DprAuthAwareAuthenticationToken? = null,
    filters: Map<String, String>? = null,
  ): DashboardDefinition {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(
      dataProductDefinitionId,
      dashboardId,
      dataProductDefinitionsPath,
    )

    return dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = productDefinition.dashboard,
      allDatasets = productDefinition.allDatasets,
      userToken = userToken,
      filters = filters,
    )
  }
}
