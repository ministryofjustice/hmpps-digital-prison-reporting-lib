package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class ProductCollectionAttribute(
  @Column(name = "attribute_name", nullable = false)
  val attributeName: String,
  @Column(name = "attribute_value", nullable = false)
  val attributeValue: String,
)
