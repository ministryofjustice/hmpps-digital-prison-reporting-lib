package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class ProductCollectionProduct(
  @Column(name = "product_id", nullable = false)
  val productId: String,
)
