package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report

@Component
class ReportDefinitionSummaryMapper(val dashboardDefinitionService: DashboardDefinitionService) {

  fun map(
    productDefinition: ProductDefinition,
    renderMethod: RenderMethod?,
  ): ReportDefinitionSummary = ReportDefinitionSummary(
    id = productDefinition.id,
    name = productDefinition.name,
    description = productDefinition.description,
    variants = productDefinition.report
      .filter { renderMethod == null || it.render.toString() == renderMethod.toString() }
      .map { map(it) },
    dashboards = map(productDefinition.dashboards),
  )

  private fun map(dashboards: List<Dashboard>?): List<DashboardDefinition>? =
    dashboards?.map { dashboardDefinitionService.toDashboardDefinition(it) }

  private fun map(
    report: Report,
  ): VariantDefinitionSummary {
    return VariantDefinitionSummary(
      id = report.id,
      name = report.name,
      description = report.description,
    )
  }
}
