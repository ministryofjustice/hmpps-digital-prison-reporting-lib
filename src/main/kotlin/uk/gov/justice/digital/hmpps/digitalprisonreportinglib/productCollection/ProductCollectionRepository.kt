package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProductCollectionRepository(
  private val entityManager: EntityManager,
) {
  companion object {
    const val FIND_ALL_QUERY = """
      WITH attribute_checks AS (
        SELECT
          product_collection_id,
          attribute_name,
          MAX(CASE
            WHEN attribute_name = 'caseloads' AND attribute_value IN :caseloads THEN 'pass'
            ELSE 'fail'
          END) AS status
        FROM product_.product_collection_attributes
        WHERE attribute_name in ('caseloads')
        GROUP BY product_collection_id, attribute_name
      ),
      failing_attributes AS (
        SELECT DISTINCT product_collection_id
        FROM attribute_checks
        WHERE status = 'fail'
      )
      SELECT DISTINCT pc.id, pc.name, pc.version, pc.owner_name, pca.attribute_name, pca.attribute_value 
      FROM product_.product_collection pc
      LEFT JOIN product_.product_collection_attributes pca on pc.id = pca.product_collection_id
      WHERE pc.id NOT IN (
        SELECT product_collection_id FROM failing_attributes
      )
    """
  }

  fun findAll(caseloads: List<String>): Collection<ProductCollectionResult> = entityManager.createNativeQuery(
    FIND_ALL_QUERY,
    ProductCollectionResult::class.java,
  ).setParameter("caseloads", caseloads).resultList.filterIsInstance<ProductCollectionResult>()

  fun findById(id: String): ProductCollection? = entityManager.find(ProductCollection::class.java, id)

  fun flush() = entityManager.flush()

  @Transactional(value = "transactionManager")
  fun save(productCollection: ProductCollection): ProductCollection = entityManager.merge(productCollection)
}
