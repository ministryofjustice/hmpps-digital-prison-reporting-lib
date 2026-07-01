package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class LoadedDefinitions(
  val summaries: List<ProductDefinitionSummary>,
  val definitionsById: Map<String, ProductDefinition>,
)
