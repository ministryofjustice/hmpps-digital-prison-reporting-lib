package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollection
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollectionResult

interface ProductCollectionRepository : JpaRepository<ProductCollection, String> {
  @Query(
    """
      WITH attribute_checks AS (
        SELECT
          product_collection_id,
          attribute_name,
          MAX(CASE
            WHEN attribute_name = 'caseloads' AND attribute_value IN :caseloads THEN 'pass'
            ELSE 'fail'
          END) AS status
        FROM product_.product_collection_attributes
        WHERE attribute_name in ('role', 'caseloads')
        GROUP BY product_collection_id, attribute_name
      ),
      failing_attributes AS (
        SELECT DISTINCT product_collection_id
        FROM attribute_checks
        WHERE status = 'fail'
      )
      SELECT DISTINCT pc.id, pc.name, pc.version, pc.owner_name, pca.attribute_name, pca.attribute_value, pcp.product_id 
      FROM product_.product_collection pc
      LEFT JOIN product_.product_collection_attributes pca on pc.id = pca.product_collection_id
      LEFT JOIN product_.product_collection_products pcp on pc.id = pcp.product_collection_id
      WHERE pc.id NOT IN (
        SELECT product_collection_id FROM failing_attributes
      )
    """,
    nativeQuery = true,
  )
  fun findAllCollections(caseloads: List<String>): Collection<ProductCollectionResult>
}
