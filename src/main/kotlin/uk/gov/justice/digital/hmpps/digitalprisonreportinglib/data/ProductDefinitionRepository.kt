package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

interface ProductDefinitionRepository {

  fun getProductDefinitions(): List<ProductDefinition>

  fun getProductDefinition(definitionId: String): ProductDefinition

  fun getSingleReportProductDefinition(definitionId: String, reportId: String): SingleReportProductDefinition
}
