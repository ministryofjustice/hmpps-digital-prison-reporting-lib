package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Validated
@RestController
@Tag(name = "Product Collection API")
class ProductCollectionController(val productCollectionService: ProductCollectionService) {

  @GetMapping("/productCollections")
  @Operation(
    description = "Gets all product collections",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getCollections(authentication: Authentication): Collection<ProductCollectionSummary> = productCollectionService.getProductCollections(
    userToken = authentication as? DprAuthAwareAuthenticationToken,
  )

  @GetMapping("/productCollections/{id}")
  @Operation(
    description = "Gets product collection by id",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getCollections(
    @Parameter(
      description = "The ID of the product collection.",
      example = "72c22579-3f77-4e23-8d16-1e5aadcc88c9",
    )
    @PathVariable("id")
    id: String,
  ): ProductCollectionDTO = productCollectionService.findById(id)
}
