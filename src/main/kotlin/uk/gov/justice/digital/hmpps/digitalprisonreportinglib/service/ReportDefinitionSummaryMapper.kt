package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Component
class ReportDefinitionSummaryMapper {

  fun map(
    productDefinition: ProductDefinition,
    renderMethod: RenderMethod?,
    userToken: DprAuthAwareAuthenticationToken?,
  ): ReportDefinitionSummary = ReportDefinitionSummary(
    id = productDefinition.id,
    name = productDefinition.name,
    description = productDefinition.description,
    variants = productDefinition.report
      .filter { renderMethod == null || it.render.toString() == renderMethod.toString() }
      .map { map(it, productDefinition.path == DataDefinitionPath.MISSING) },
    dashboards = productDefinition.dashboard?.map { map(it) },
    authorised = determineAuth(productDefinition, userToken),
  )

  private fun determineAuth(
    productDefinition: ProductDefinition,
    userToken: DprAuthAwareAuthenticationToken?,
  ): Boolean = ProductDefinitionTokenPolicyChecker().determineAuth(productDefinition, userToken)

  private fun map(
    report: Report,
    isMissing: Boolean
  ): VariantDefinitionSummary = VariantDefinitionSummary(
    id = report.id,
    name = report.name,
    description = report.description,
    isMissing,
  )

  private fun map(
    dashboard: Dashboard,
  ): DashboardDefinitionSummary = DashboardDefinitionSummary(
    id = dashboard.id,
    name = dashboard.name,
    description = dashboard.description,
  )
}
