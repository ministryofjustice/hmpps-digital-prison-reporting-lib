package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductCollectionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Service
class ProductCollectionService(
  val productCollectionRepository: ProductCollectionRepository,
) {
  fun getProductCollections(userToken: DprAuthAwareAuthenticationToken?): Collection<ProductCollectionSummary> {
    // Set this to "NULL" if theres no values, which will fail the check, if a collection has any caseloadId attributes linked to it
    val caseloadIds = userToken?.getCaseLoadIds()?.takeIf { it.isNotEmpty() } ?: listOf("NULL")
    val groupBy = productCollectionRepository.findAll(caseloadIds)
      .groupBy { it.id }
      .values
    val results = groupBy
      .map { entry ->
        val firstEntry = entry.firstOrNull()
        if (firstEntry == null) {
          throw IllegalStateException("After grouping by productCollection id, somehow list of collections was empty")
        }
        listOf(ProductCollectionSummary(firstEntry.id, firstEntry.name, firstEntry.version, firstEntry.owner_name))
      }
      .flatten()
    return results
  }

  fun findById(id: String): ProductCollectionDTO {
    val result = productCollectionRepository.findById(id).orElseThrow { ValidationException("Invalid product collection id specified: $id") }
    return result.let { ProductCollectionDTO(it.id!!, it.name, it.version, it.ownerName, it.products) }
  }
}
