package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

class ClientDataProductDefinitionsRepository(
  private val dataProductDefinitionsClient: RestTemplate,
  private val definitionsHost: String?,
) : AbstractProductDefinitionRepository() {

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    if (definitionsHost == null) {
      return emptyList()
    }
    val respEntity: ResponseEntity<List<ProductDefinition>>? = path?.let {
      dataProductDefinitionsClient
        .exchange("$definitionsHost/$path", HttpMethod.GET, null, object : ParameterizedTypeReference<List<ProductDefinition>>() {})
    }
    return respEntity?.body ?: emptyList()
  }
}
