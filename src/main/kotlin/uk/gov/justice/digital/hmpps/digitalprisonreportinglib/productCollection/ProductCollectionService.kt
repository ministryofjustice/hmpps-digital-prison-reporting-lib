package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductCollectionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Service
class ProductCollectionService(
  val productCollectionRepository: ProductCollectionRepository,
) {
  fun getProductCollections(userToken: DprAuthAwareAuthenticationToken?): Collection<ProductCollectionDTO> {
    // Set this to "NULL" if theres no values, which will fail the check, if a collection has any caseloadId attributes linked to it
    val caseloadIds = userToken?.getCaseLoadIds()?.takeIf { it.isNotEmpty() } ?: listOf("NULL")
    val groupBy = productCollectionRepository.findAllCollections(caseloadIds)
      .groupBy { it.id }
      .values
    val results = groupBy
      .map { entry ->
        val firstEntry = entry.firstOrNull()
        if (firstEntry == null) {
          throw IllegalStateException("After grouping by productCollection id, somehow list of collections was empty")
        }
        val products = entry.filter { it.product_id != null }.map { ProductCollectionProduct(it.product_id!!) }
        listOf(ProductCollectionDTO(firstEntry.id, firstEntry.name, firstEntry.version, firstEntry.owner_name, products))
      }
      .flatten()
    return results
  }
}
