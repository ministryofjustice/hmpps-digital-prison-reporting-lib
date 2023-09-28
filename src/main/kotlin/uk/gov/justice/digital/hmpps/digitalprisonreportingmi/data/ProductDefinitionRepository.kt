package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ProductDefinition

interface ProductDefinitionRepository {

  fun getProductDefinitions(): List<ProductDefinition>
}
