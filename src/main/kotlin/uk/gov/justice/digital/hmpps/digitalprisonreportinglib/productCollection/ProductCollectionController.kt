package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
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
  fun getCollections(authentication: Authentication): Collection<ProductCollectionDTO> = productCollectionService.getProductCollections(
    userToken = authentication as? DprAuthAwareAuthenticationToken,
  )
}
