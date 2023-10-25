package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

interface ProductDefinitionRepository {

  fun getProductDefinitions(): List<ProductDefinition>
}
