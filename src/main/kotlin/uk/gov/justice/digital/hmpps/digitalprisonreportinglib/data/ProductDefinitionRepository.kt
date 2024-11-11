package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

interface ProductDefinitionRepository {

  fun getProductDefinitions(path: String? = null): List<ProductDefinition>

  fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String? = null): ProductDefinition

  fun getSingleReportProductDefinition(definitionId: String, reportId: String, dataProductDefinitionsPath: String? = null): SingleReportProductDefinition
}
