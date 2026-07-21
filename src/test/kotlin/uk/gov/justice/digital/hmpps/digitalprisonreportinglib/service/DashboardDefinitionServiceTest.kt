package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class DashboardDefinitionServiceTest {

  private val productDefinitionRepository: ProductDefinitionRepository = mock()
  private val dashboardDefinitionMapper: DashboardDefinitionMapper = mock()
  private val productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker = mock()

  private val dashboardDefinitionService = DashboardDefinitionService(productDefinitionRepository, dashboardDefinitionMapper, productDefinitionTokenPolicyChecker)
  private val executionContext = ExecutionContext(
    CaseloadResponse(
      username = "request-user",
      active = true,
      accountType = "GENERAL",
      caseloads = listOf(
        Caseload("KMI", "KIRKHAM"),
        Caseload("WWI", "WANDSWORTH (HMP)"),
      ),
      activeCaseload = Caseload(id = "WWI", name = "WANDSWORTH (HMP)"),
    ),
    listOf("ROLE_PRISONS_REPORTING_USER"),
    AuthUser("request-user", true, "request-user", AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
    false,
  )

  @Test
  fun `getDashboardDefinition returns the dashboard definition`() {
    val dashboardDefinition: DashboardDefinition = mock()
    val productDefinition: SingleDashboardProductDefinition = mock()
    val dashboard: Dashboard = mock()
    val allDatasets: List<Dataset> = listOf(mock())
    val definitionId = "missing-ethnicity-metrics"
    val dashboardId = "age-breakdown-dashboard-1"

    whenever(dashboardDefinitionMapper.toDashboardDefinition(any(), any(), any(), anyOrNull())).doReturn(dashboardDefinition)
    whenever(productDefinitionTokenPolicyChecker.determineAuth(any(), any())).doReturn(true)
    whenever(productDefinitionRepository.getSingleDashboardProductDefinition(any(), any(), anyOrNull())).doReturn(productDefinition)
    whenever(productDefinition.dashboard).doReturn(dashboard)
    whenever(productDefinition.allDatasets).doReturn(allDatasets)

    val actual = dashboardDefinitionService.getDashboardDefinition(
      dataProductDefinitionId = definitionId,
      dashboardId = dashboardId,
      executionContext,
    )

    assertThat(actual).isEqualTo(dashboardDefinition)

    verify(productDefinitionRepository).getSingleDashboardProductDefinition(definitionId, dashboardId)
    verify(dashboardDefinitionMapper).toDashboardDefinition(dashboard, listOf(dashboard), allDatasets, executionContext)
  }
}
