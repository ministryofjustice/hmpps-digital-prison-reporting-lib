package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

/**
 * We have this because otherwise hibermate has to go and get entries for each row identified by the cross-joins from
 * findAllCollections if we were to use the entity itself, leading to an explosion of queries if there are many
 * attributes and/or products
 */
@Suppress("ktlint:standard:property-naming") // field names need to match db col names exactly
interface ProductCollectionResult {
  val id: String
  val name: String
  val version: String
  val owner_name: String
  val product_id: String?
  val attribute_name: String?
  val attribute_value: String?
}
