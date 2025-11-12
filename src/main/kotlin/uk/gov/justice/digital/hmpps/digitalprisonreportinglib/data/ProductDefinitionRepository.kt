package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.AnyProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

interface ProductDefinitionRepository {

  fun getProductDefinitions(path: String? = null): List<AnyProductDefinition>

  fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String? = null): AnyProductDefinition

  fun getSingleReportProductDefinition(definitionId: String, reportId: String, dataProductDefinitionsPath: String? = null): SingleReportProductDefinition

  fun getSingleDashboardProductDefinition(definitionId: String, dashboardId: String, dataProductDefinitionsPath: String? = null): SingleDashboardProductDefinition
}
