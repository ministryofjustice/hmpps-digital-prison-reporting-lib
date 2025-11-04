package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

data class ProductCollectionDTO(
  val id: String,
  val name: String,
  val version: String,
  val ownerName: String,
  val products: Collection<ProductCollectionProduct>,
)
