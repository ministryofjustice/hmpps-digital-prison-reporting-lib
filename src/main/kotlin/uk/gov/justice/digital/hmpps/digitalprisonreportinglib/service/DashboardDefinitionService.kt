package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException

@Service
class DashboardDefinitionService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val dashboardDefinitionMapper: DashboardDefinitionMapper,
  val productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker,
) {
  fun getDashboardDefinition(
    dataProductDefinitionId: String,
    dashboardId: String,
    executionContext: ExecutionContext,
    dataProductDefinitionsPath: String? = null,
    filters: Map<String, String>? = null,
  ): DashboardDefinition {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(
      dataProductDefinitionId,
      dashboardId,
      dataProductDefinitionsPath,
    )
    checkAuth(productDefinition, executionContext)

    return dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = productDefinition.dashboard,
      allDatasets = productDefinition.allDatasets,
      executionContext = executionContext,
      filters = filters,
    )
  }

  private fun checkAuth(
    productDefinition: WithPolicy,
    executionContext: ExecutionContext,
  ): Boolean {
    if (!productDefinitionTokenPolicyChecker.determineAuth(productDefinition, executionContext)) {
      throw UserAuthorisationException("User does not have correct authorisation")
    }
    return true
  }
}
