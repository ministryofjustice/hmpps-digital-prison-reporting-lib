package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

data class ProductCollection(
  val id: String? = null,
  val name: String,
  val version: String,
  val ownerName: String,
  val products: MutableSet<ProductCollectionProduct>,
  val attributes: MutableSet<ProductCollectionAttribute>,
) {
  constructor(
    name: String,
    version: String,
    ownerName: String,
    products: MutableSet<ProductCollectionProduct>,
    attributes: MutableSet<ProductCollectionAttribute>,
  ) : this(null, name, version, ownerName, products, attributes)
}
