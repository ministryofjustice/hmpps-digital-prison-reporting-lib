package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

/**
 * We have this because otherwise hibermate has to go and get entries for each row identified by the cross-joins from
 * findAll if we were to use the entity itself, leading to an explosion of queries if there are many
 * attributes and/or products
 */

data class ProductCollectionResult(
  val id: String,
  val name: String,
  val version: String,
  val ownerName: String,
  val attributeName: String?,
  val attributeValue: String?,
)
