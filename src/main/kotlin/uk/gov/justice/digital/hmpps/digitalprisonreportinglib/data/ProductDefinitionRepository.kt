package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

interface ProductDefinitionRepository {

  suspend fun getProductDefinitions(path: String? = null): List<ProductDefinition>

  suspend fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String? = null): ProductDefinition

  suspend fun getSingleReportProductDefinition(definitionId: String, reportId: String, dataProductDefinitionsPath: String? = null): SingleReportProductDefinition

  suspend fun getSingleDashboardProductDefinition(definitionId: String, dashboardId: String, dataProductDefinitionsPath: String? = null): SingleDashboardProductDefinition
}
