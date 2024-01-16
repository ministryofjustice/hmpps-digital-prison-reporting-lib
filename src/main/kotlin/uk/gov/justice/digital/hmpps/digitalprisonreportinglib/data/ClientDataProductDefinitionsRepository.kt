package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

class ClientDataProductDefinitionsRepository(
  private val dataProductDefinitionsClient: RestTemplate,
  private val definitionsHost: String?,
  private val definitionsCache: Cache<String, List<ProductDefinition>>?,
) : AbstractProductDefinitionRepository() {

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    if (definitionsHost == null) {
      return emptyList()
    }
    val cachedDefinition = definitionsCache?.let { cache ->
      path?.let { path -> cache.getIfPresent(path) }
    }
    cachedDefinition?.let { return it }
    val respEntity: ResponseEntity<List<ProductDefinition>>? = path?.let {
      dataProductDefinitionsClient
        .exchange("$definitionsHost/$path", HttpMethod.GET, null, object : ParameterizedTypeReference<List<ProductDefinition>>() {})
    }
    return respEntity?.body?.let { responseBody ->
      definitionsCache?.put(path, responseBody)
      responseBody
    } ?: emptyList()
  }
}
