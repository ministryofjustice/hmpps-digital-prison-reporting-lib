package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

interface ProductDefinitionRepository {

  fun getProductDefinitions(): List<ProductDefinitionSummary>

  fun getProductDefinition(definitionId: String): ProductDefinition

  fun getSingleReportProductDefinition(definitionId: String, reportId: String): SingleReportProductDefinition

  fun getSingleDashboardProductDefinition(definitionId: String, dashboardId: String): SingleDashboardProductDefinition
}
