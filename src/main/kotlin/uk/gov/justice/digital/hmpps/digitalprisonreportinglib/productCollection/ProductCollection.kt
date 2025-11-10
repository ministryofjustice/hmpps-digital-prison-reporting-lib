package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table

@Entity
@Table(schema = "product_", name = "product_collection")
@SqlResultSetMapping(
  name = "ProductCollectionResult",
  classes = [
    ConstructorResult(
      targetClass = ProductCollectionResult::class,
      columns = [ColumnResult(name = "id"), ColumnResult(name = "name"), ColumnResult(name = "version"), ColumnResult(name = "owner_name"), ColumnResult(name = "attribute_name"), ColumnResult(name = "attribute_value")],
    ),
  ],
)
class ProductCollection(
  @Column(nullable = false)
  val name: String,

  @Column(nullable = false)
  val version: String,

  @Column(nullable = false)
  val ownerName: String,

  @ElementCollection
  @CollectionTable(
    schema = "product_",
    name = "product_collection_products",
    joinColumns = [JoinColumn(name = "product_collection_id")],
  )
  val products: Set<ProductCollectionProduct>,

  @ElementCollection
  @CollectionTable(
    schema = "product_",
    name = "product_collection_attributes",
    joinColumns = [JoinColumn(name = "product_collection_id")],
  )
  val attributes: Set<ProductCollectionAttribute>,
) {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  val id: String? = null
}
