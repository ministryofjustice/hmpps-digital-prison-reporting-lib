package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.AnyProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

abstract class AbstractProductDefinitionRepository(
  private val identifiedHelper: IdentifiedHelper,
) : ProductDefinitionRepository {

  override fun getSingleReportProductDefinition(
    definitionId: String,
    reportId: String,
    dataProductDefinitionsPath: String?,
  ): SingleReportProductDefinition {
    val productDefinition: ProductDefinition = getProductDefinition(definitionId, dataProductDefinitionsPath)
    val reportDefinition = identifiedHelper.findOrFail(productDefinition.report, reportId)
    val dataSet = identifiedHelper.findOrFail(productDefinition.dataset, reportDefinition.dataset)
    reportDefinition.specification?.field?.forEach { specField -> identifiedHelper.findOrFail(dataSet.schema.field, specField.name) }
    return SingleReportProductDefinition(
      id = definitionId,
      name = productDefinition.name,
      description = productDefinition.description,
      scheduled = productDefinition.scheduled,
      metadata = productDefinition.metadata,
      datasource = findDatasource(productDefinition, dataSet, definitionId),
      reportDataset = dataSet,
      allDatasets = productDefinition.dataset,
      report = reportDefinition,
      policy = productDefinition.policy,
      allReports = productDefinition.report,
      allDatasources = productDefinition.datasource,
    )
  }

  override fun getSingleDashboardProductDefinition(
    definitionId: String,
    dashboardId: String,
    dataProductDefinitionsPath: String?,
  ): SingleDashboardProductDefinition {
    val productDefinition: AnyProductDefinition = getProductDefinition(definitionId, dataProductDefinitionsPath)
    val dashboard = productDefinition.dashboard?.firstOrNull { it.id == dashboardId }
      ?: throw ValidationException("Invalid report dashboard id provided: $dashboardId")
    val dataSet = identifiedHelper.findOrFail(productDefinition.dataset, dashboard.dataset)

    return SingleDashboardProductDefinition(
      id = definitionId,
      name = productDefinition.name,
      description = productDefinition.description,
      metadata = productDefinition.metadata,
      datasource = findDatasource(productDefinition, dataSet, definitionId),
      policy = productDefinition.policy,
      dashboardDataset = dataSet,
      dashboard = dashboard,
      allDashboards = productDefinition.dashboard!!,
      allDatasets = productDefinition.dataset,
      allDatasources = productDefinition.datasource,
    )
  }
  private fun findDatasource(
    productDefinition: AnyProductDefinition,
    dataSet: Dataset,
    definitionId: String,
  ): Datasource = (
    identifiedHelper
      .findOrNull(productDefinition.datasource, dataSet.datasource)
      ?: productDefinition.datasource.firstOrNull() // Maintaining the existing behaviour as existing reports could break if this line is removed.
      ?: throw ValidationException("No datasource provided for definition with ID: $definitionId.")
    )
}
