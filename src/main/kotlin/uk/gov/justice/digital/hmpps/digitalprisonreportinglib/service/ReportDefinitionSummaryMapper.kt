package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.AnyProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.AnyReport
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy

@Component
class ReportDefinitionSummaryMapper {

  fun map(
    productDefinition: AnyProductDefinition,
    renderMethod: RenderMethod?,
  ): ReportDefinitionSummary = ReportDefinitionSummary(
    id = productDefinition.id,
    name = productDefinition.name,
    description = productDefinition.description,
    variants = productDefinition.report
      .filter { renderMethod == null || it.render.toString() == renderMethod.toString() }
      .map { map(it, productDefinition.path == DataDefinitionPath.MISSING) },
    dashboards = productDefinition.dashboard?.map { map(it) },
    authorised = determineAuth(productDefinition),
  )

  private fun determineAuth(
    productDefinition: WithPolicy,
  ): Boolean = ProductDefinitionTokenPolicyChecker().determineAuth(productDefinition)

  private fun map(
    report: AnyReport,
    isMissing: Boolean,
  ): VariantDefinitionSummary = VariantDefinitionSummary(
    id = report.id,
    name = report.name,
    description = report.description,
    isMissing,
    loadType = report.loadType,
  )

  private fun map(
    dashboard: Dashboard,
  ): DashboardDefinitionSummary = DashboardDefinitionSummary(
    id = dashboard.id,
    name = dashboard.name,
    description = dashboard.description,
    loadType = dashboard.loadType,
  )
}
