package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DynamoDbProductDefinitionProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthenticationHelper
import java.util.concurrent.TimeUnit

@Configuration
class ProductDefinitionRepositoryAutoConfig(
  @Value("\${dpr.lib.definition.locations:#{null}}") private val definitionResourceLocations: List<String>?,
  @Value("\${dpr.lib.dataProductDefinitions.host:#{null}}") private val definitionsHost: String?,
  @Value("\${dpr.lib.dataProductDefinitions.cache.durationMinutes:#{30}}") private val cacheDurationMinutes: Long,
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
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  fun dataProductDefinitionsRepository(
    dprDefinitionGson: Gson,
    definitionsCache: Cache<String, List<ProductDefinition>>? = null,
    authenticationHelper: AuthenticationHelper,
  ): ProductDefinitionRepository = ClientDataProductDefinitionsRepository(
    RestTemplate(
      listOf(GsonHttpMessageConverter(dprDefinitionGson)),
    ),
    definitionsHost,
    definitionsCache,
    authenticationHelper,
  )

  @Bean
  @ConditionalOnProperty("dpr.lib.dataproductdefinitions.dynamodb.enabled", havingValue = "true")
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  fun dynamoDbProductDefinitionsRepository(
    dprDefinitionGson: Gson,
    dynamoDbClient: DynamoDbClient,
    properties: DynamoDbProductDefinitionProperties,
  ): ProductDefinitionRepository = DynamoDbProductDefinitionRepository(
    dynamoDbClient = dynamoDbClient,
    gson = dprDefinitionGson,
    properties = properties,
  )

  @Bean
  @ConditionalOnProperty("dpr.lib.dataProductDefinitions.cache.enabled", havingValue = "true")
  fun definitionsCache(): Cache<String, List<ProductDefinition>> = CacheBuilder.newBuilder()
    .expireAfterWrite(cacheDurationMinutes, TimeUnit.MINUTES)
    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
    .build()

  @Bean
  @ConditionalOnMissingBean(LocalDateTimeTypeAdaptor::class)
  fun localDateTimeTypeAdaptor(): LocalDateTimeTypeAdaptor {
    return IsoLocalDateTimeTypeAdaptor()
  }
}
