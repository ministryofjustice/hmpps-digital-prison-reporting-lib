package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.LoadedDefinitions
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
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
    identifiedHelper: IdentifiedHelper,
  ): ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    definitionResourceLocations ?: emptyList(),
    dprDefinitionGson,
    identifiedHelper,
  )

  @Bean
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  @ConditionalOnBean(DynamoDbClient::class)
  @ConditionalOnProperty(
    "dpr.lib.dataProductDefinitions.s3.enabled",
    havingValue = "false",
    matchIfMissing = true,
  )
  fun dynamoDbProductDefinitionsRepository(
    dprDefinitionGson: Gson,
    dynamoDbClient: DynamoDbClient,
    properties: AwsProperties,
    definitionsCache: Cache<String, List<ProductDefinitionSummary>>?,
    identifiedHelper: IdentifiedHelper,
  ): ProductDefinitionRepository = DynamoDbProductDefinitionRepository(
    dynamoDbClient = dynamoDbClient,
    gson = dprDefinitionGson,
    properties = properties,
    definitionsCache = definitionsCache,
    identifiedHelper = identifiedHelper,
  )

  @Bean
  @ConditionalOnProperty("dpr.lib.dataProductDefinitions.cache.enabled", havingValue = "true")
  @ConditionalOnProperty("dpr.lib.dataProductDefinitions.s3.enabled", havingValue = "false", matchIfMissing = true)
  fun definitionsCache(): Cache<String, List<ProductDefinitionSummary>> = CacheBuilder.newBuilder()
    .expireAfterWrite(cacheDurationMinutes, TimeUnit.MINUTES)
    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
    .build()

  @Bean
  @ConditionalOnProperty(
    value = ["dpr.lib.dataProductDefinitions.cache.enabled", "dpr.lib.dataProductDefinitions.s3.enabled"],
    havingValue = "true",
  )
  fun s3AndDdbDefinitionsCache(): Cache<String, LoadedDefinitions> = CacheBuilder.newBuilder()
    .expireAfterWrite(cacheDurationMinutes, TimeUnit.MINUTES)
    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
    .build()

  @Bean
  @ConditionalOnMissingBean(ProductDefinitionRepository::class)
  @ConditionalOnProperty("dpr.lib.dataProductDefinitions.s3.enabled", havingValue = "true")
  @ConditionalOnBean(DynamoDbClient::class, S3Client::class)
  fun s3AndDynamoDbProductDefinitionsRepository(
    dprDefinitionGson: Gson,
    dynamoDbClient: DynamoDbClient,
    s3Client: S3Client,
    properties: AwsProperties,
    s3AndDdbDefinitionsCache: Cache<String, LoadedDefinitions>?,
    identifiedHelper: IdentifiedHelper,
    @Value("\${dpr.lib.dataProductDefinitions.s3.bucket}")
    s3Bucket: String,
  ): ProductDefinitionRepository = S3AndDynamoDbProductDefinitionRepository(
    dynamoDbClient = dynamoDbClient,
    s3Client = s3Client,
    gson = dprDefinitionGson,
    properties = properties,
    s3Bucket = s3Bucket,
    s3AndDdbDefinitionsCache = s3AndDdbDefinitionsCache,
    identifiedHelper = identifiedHelper,
  )

  @Bean
  @ConditionalOnMissingBean(LocalDateTimeTypeAdaptor::class)
  fun localDateTimeTypeAdaptor(): LocalDateTimeTypeAdaptor = IsoLocalDateTimeTypeAdaptor()
}
