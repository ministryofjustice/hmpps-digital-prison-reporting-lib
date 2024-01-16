package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import java.util.concurrent.TimeUnit

@Configuration
class ProductDefinitionRepositoryAutoConfig(
  @Value("\${dpr.lib.definition.locations:#{null}}") private val definitionResourceLocations: List<String>?,
  @Value("\${dpr.lib.dataProductDefinitions.host:#{null}}") private val definitionsHost: String?,
  @Value("\${dpr.lib.dataProductDefinitions.cacheEnabled:#{false}}") private val cacheEnabled: Boolean?,
) {

  @Bean
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  @ConditionalOnProperty(prefix = "dpr.lib.definition", name = ["locations"])
  fun productDefinitionRepository(
    localDateTimeTypeAdaptor: LocalDateTimeTypeAdaptor,
    dprDefinitionGson: Gson,
  ): ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    definitionResourceLocations ?: emptyList(),
    dprDefinitionGson,
  )

  @Bean
  @ConditionalOnExpression(
    "T(org.springframework.util.StringUtils).isEmpty('\${dpr.lib.definition.locations:}') " +
      "&& !T(org.springframework.util.StringUtils).isEmpty('\${dpr.lib.dataProductDefinitions.host:}')",
  )
  fun dataProductDefinitionsRepository(
    dprDefinitionGson: Gson,
    definitionsCache: Cache<String, List<ProductDefinition>>? = null,
  ): ProductDefinitionRepository = ClientDataProductDefinitionsRepository(
    RestTemplate(
      listOf(GsonHttpMessageConverter(dprDefinitionGson)),
    ),
    definitionsHost,
    definitionsCache,
  )

  @Bean
  @ConditionalOnProperty("dpr.lib.dataProductDefinitions.cacheEnabled", havingValue = "true")
  fun definitionsCache(): Cache<String, List<ProductDefinition>> = CacheBuilder.newBuilder()
    .expireAfterWrite(30, TimeUnit.MINUTES)
    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
    .build()

  @Bean
  @ConditionalOnMissingBean(LocalDateTimeTypeAdaptor::class)
  fun localDateTimeTypeAdaptor(): LocalDateTimeTypeAdaptor {
    return IsoLocalDateTimeTypeAdaptor()
  }
}
