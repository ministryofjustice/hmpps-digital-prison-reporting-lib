package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.*

interface ProductDefinitionRepository {

  fun getProductDefinitions(path: String? = null): List<ProductDefinitionSummary>

  fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String? = null): ProductDefinition

  fun getSingleReportProductDefinition(definitionId: String, reportId: String, dataProductDefinitionsPath: String? = null): SingleReportProductDefinition

  fun getSingleDashboardProductDefinition(definitionId: String, dashboardId: String, dataProductDefinitionsPath: String? = null): SingleDashboardProductDefinition
}
