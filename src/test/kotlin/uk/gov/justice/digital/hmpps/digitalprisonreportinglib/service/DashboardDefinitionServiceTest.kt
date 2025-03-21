package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

class DashboardDefinitionServiceTest {

  private val productDefinitionRepository: ProductDefinitionRepository = mock()
  private val dashboardDefinitionMapper: DashboardDefinitionMapper = mock()

  private val dashboardDefinitionService = DashboardDefinitionService(productDefinitionRepository, dashboardDefinitionMapper)

  @Test
  fun `getDashboardDefinition returns the dashboard definition`() {
    val dashboardDefinition: DashboardDefinition = mock()
    val userToken = mock<DprAuthAwareAuthenticationToken>()
    val productDefinition: SingleDashboardProductDefinition = mock()
    val dashboard: Dashboard = mock()
    val allDatasets: List<Dataset> = listOf(mock())
    val definitionId = "missing-ethnicity-metrics"
    val dashboardId = "age-breakdown-dashboard-1"

    whenever(dashboardDefinitionMapper.toDashboardDefinition(any(), any(), any())).doReturn(dashboardDefinition)
    whenever(productDefinitionRepository.getSingleDashboardProductDefinition(any(), any(), anyOrNull())).doReturn(productDefinition)
    whenever(productDefinition.dashboard).doReturn(dashboard)
    whenever(productDefinition.allDatasets).doReturn(allDatasets)

    val actual = dashboardDefinitionService.getDashboardDefinition(
      dataProductDefinitionId = definitionId,
      dashboardId = dashboardId,
      userToken = userToken,
    )

    assertThat(dashboardDefinition).isEqualTo(actual)

    verify(productDefinitionRepository).getSingleDashboardProductDefinition(definitionId, dashboardId)
    verify(dashboardDefinitionMapper).toDashboardDefinition(dashboard, allDatasets, userToken)
  }
}
