package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import java.net.URI

class ClientDataProductDefinitionsRepository(
  private val dataProductDefinitionsClient: RestTemplate,
  private val definitionsHost: String?,
  private val definitionsCache: Cache<String, List<ProductDefinition>>?,
) : AbstractProductDefinitionRepository() {

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    if (definitionsHost == null) {
      return emptyList()
    }
    val cachedDefinitions = definitionsCache?.let { cache ->
      path?.let { path -> cache.getIfPresent(path) }
    }
    cachedDefinitions?.let { return it }

    val authentication = SecurityContextHolder.getContext().authentication as DprAuthAwareAuthenticationToken
    val respEntity: ResponseEntity<List<ProductDefinition>>? = path?.let {
      val headers: MultiValueMap<String, String> = HttpHeaders()
        headers.add("Authorization", "Bearer ${authentication.jwt.tokenValue}")
      val requestEntity = RequestEntity<List<ProductDefinition>>(headers, HttpMethod.GET, URI("$definitionsHost/$path"))
      dataProductDefinitionsClient
        .exchange(requestEntity, object : ParameterizedTypeReference<List<ProductDefinition>>() {})
    }
    return respEntity?.body?.let { responseBody ->
      definitionsCache?.put(path, responseBody)
      responseBody
    } ?: emptyList()
  }
}
