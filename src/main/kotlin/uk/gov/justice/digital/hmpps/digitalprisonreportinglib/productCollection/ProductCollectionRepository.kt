package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.RowMapperResultSetExtractor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID

@Repository
class ProductCollectionRepository : RepositoryHelper() {
  companion object {
    const val FIND_ALL_QUERY = """
      WITH attribute_checks AS (
        SELECT
          product_collection_id,
          attribute_name,
          MAX(CASE
            WHEN attribute_name = 'caseloads' AND attribute_value IN (:caseloads) THEN 'pass'
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
    const val FIND_BY_ID_QUERY = """
      SELECT pc.id as id, pc.name as name, pc.version as version, pc.owner_name as owner_name, pca.attribute_name as attribute_name, pca.attribute_value as attribute_value, pcp.product_id as product_id
      FROM product_.product_collection pc
      LEFT JOIN product_.product_collection_attributes pca on pc.id = pca.product_collection_id
      LEFT JOIN product_.product_collection_products pcp on pc.id = pcp.product_collection_id
      WHERE pc.id = :id
      """

    const val DELETE_COLLECTION_ATTRIBUTES_PARTIAL = """
      DELETE FROM product_.product_collection_attributes
      WHERE product_collection_id = :id
      AND (attribute_name, attribute_value) NOT IN (
        ;;deleteAttrValues;;
      );
    """
    const val INSERT_NEW_COLLECTION_ATTRIBUTES = """
      INSERT INTO product_.product_collection_attributes
      (product_collection_id, attribute_name, attribute_value)
      VALUES ;;insertAttrValues;;;
    """

    const val DELETE_COLLECTION_PRODUCTS = """
      DELETE FROM product_.product_collection_products
      WHERE product_collection_id = :id
      AND product_id NOT IN :deleteProducts;
    """
    const val INSERT_NEW_COLLECTION_PRODUCTS = """
      INSERT INTO product_.product_collection_products
      (product_collection_id, product_id)
      VALUES ;;insertProducts;;;
    """

    const val MERGE_PRODUCT_COLLECTION = """
      MERGE INTO product_.product_collection
      USING (SELECT :id as id, :name as name, :version as version, :owner_name as owner_name) as source
      on product_.product_collection.id = source.id 
      WHEN MATCHED THEN
      UPDATE SET id = source.id, name = source.name, version = source.version, owner_name = source.owner_name
      WHEN NOT MATCHED 
      THEN INSERT VALUES (source.id, source.name, source.version, source.owner_name);
    """
  }

  fun findAll(caseloads: List<String>): Collection<ProductCollectionResult> = populateNamedParameterJdbcTemplate().query(
    FIND_ALL_QUERY,
    mapOf("caseloads" to caseloads),
    RowMapperResultSetExtractor(ProductCollectionRowMapper()),
  ).orEmpty()

  fun findById(id: String): ProductCollection? {
    val results = populateNamedParameterJdbcTemplate().query(
      FIND_BY_ID_QUERY,
      mapOf("id" to id),
      RowMapperResultSetExtractor(ProductCollectionRowMapper()),
    )
    if (results.isNullOrEmpty()) {
      return null
    }
    val firstResult = results[0]!!
    val collection = ProductCollection(firstResult.id, firstResult.name, firstResult.version, firstResult.ownerName, mutableSetOf(), mutableSetOf())
    results.forEach {
      if (it.productId != null) {
        collection.products.add(ProductCollectionProduct(it.productId))
      }
      if (it.attributeName != null && it.attributeValue != null) {
        collection.attributes.add(ProductCollectionAttribute(it.attributeName, it.attributeValue))
      }
    }
    return collection
  }

  fun save(productCollection: ProductCollection): ProductCollection {
    val template = populateNamedParameterJdbcTemplate()
    var deleteAttributesQuery = ""
    var insertAttributesQuery = ""

    var deleteProductsQuery = ""
    var insertProductsQuery = ""

    val namedParameters = mutableMapOf<String, Any>("name" to productCollection.name, "version" to productCollection.version, "owner_name" to productCollection.ownerName)

    if (!productCollection.id.isNullOrBlank()) {
      namedParameters["id"] = productCollection.id
    } else {
      namedParameters["id"] = UUID.randomUUID().toString()
    }
    if (!productCollection.id.isNullOrBlank() && productCollection.attributes.isNotEmpty()) {
      val attributesToSql = productCollection.attributes
        .mapIndexed { index, attribute ->
          namedParameters["deleteAttrValuesName$index"] = attribute.attributeName
          namedParameters["deleteAttrValuesVal$index"] = attribute.attributeValue
          "(:deleteAttrValuesName$index, :deleteAttrValuesVal$index)"
        }
        .joinToString(",")
      deleteAttributesQuery = DELETE_COLLECTION_ATTRIBUTES_PARTIAL.replace(";;deleteAttrValues;;", attributesToSql)
    }
    if (productCollection.attributes.isNotEmpty()) {
      val attributesToSql = productCollection.attributes
        .mapIndexed { index, attribute ->
          namedParameters["insertAttrValuesName$index"] = attribute.attributeName
          namedParameters["insertAttrValuesVal$index"] = attribute.attributeValue
          "(:id, :insertAttrValuesName$index, :insertAttrValuesVal$index)"
        }
        .joinToString(",")
      insertAttributesQuery = INSERT_NEW_COLLECTION_ATTRIBUTES.replace(";;insertAttrValues;;", attributesToSql)
    }

    if (!productCollection.id.isNullOrBlank() && productCollection.products.isNotEmpty()) {
      deleteProductsQuery = DELETE_COLLECTION_PRODUCTS
      namedParameters["deleteProducts"] = productCollection.products
    }
    if (productCollection.products.isNotEmpty()) {
      val productsToSql = productCollection.products
        .mapIndexed { index, product ->
          namedParameters["insertProduct$index"] = product.productId
          "(:id, :insertProduct$index)"
        }
        .joinToString(",")
      insertProductsQuery = INSERT_NEW_COLLECTION_PRODUCTS.replace(";;insertProducts;;", productsToSql)
    }

    template.update(
      """
      BEGIN READ WRITE;
        $MERGE_PRODUCT_COLLECTION

        $deleteAttributesQuery
        $insertAttributesQuery
        
        $deleteProductsQuery
        $insertProductsQuery
      COMMIT TRANSACTION
      """.trimIndent(),
      namedParameters,
    )

    return findById(namedParameters["id"]!! as String)!!
  }
}

class ProductCollectionRowMapper : RowMapper<ProductCollectionResult> {
  override fun mapRow(
    rs: ResultSet,
    rowNum: Int,
  ): ProductCollectionResult? {
    val id = rs.getString("id")
    val name = rs.getString("name")
    val version = rs.getString("version")
    val ownerName = rs.getString("owner_name")
    val optionalCols = try {
      mapOf(
        "product_id" to rs.getString("product_id"),
        "attribute_name" to rs.getString("attribute_name"),
        "attribute_value" to rs.getString("attribute_value"),
      )
    } catch (_: SQLException) {
      // ignore - we're fine with these cols not being there, but it throws an exception when they aren't
      emptyMap<String, String>()
    }
    return ProductCollectionResult(id, name, version, ownerName, optionalCols["product_id"], optionalCols["attribute_name"], optionalCols["attribute_value"])
  }
}
