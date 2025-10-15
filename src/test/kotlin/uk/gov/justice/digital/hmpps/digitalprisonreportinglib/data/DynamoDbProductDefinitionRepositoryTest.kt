package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.CacheBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.ScanResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DynamoDbProductDefinitionRepository.Companion.getScanRequest
import java.util.concurrent.TimeUnit

class DynamoDbProductDefinitionRepositoryTest {

  private val dynamoDbClient = mock<DynamoDbClient>()
  private val properties = AwsProperties(
    dynamoDb = AwsProperties.DynamoDb(),
    sts = AwsProperties.Sts(),
  )

  private val repo = DynamoDbProductDefinitionRepository(
    dynamoDbClient = dynamoDbClient,
    gson = DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    properties = properties,
    identifiedHelper = IdentifiedHelper(),
    definitionsCache = CacheBuilder.newBuilder()
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .concurrencyLevel(Runtime.getRuntime().availableProcessors())
      .build(),
  )

  @BeforeEach
  fun setup() {
    val response = mock<ScanResponse>()
    given(dynamoDbClient.scan(any(ScanRequest::class.java))).willReturn(response)
    given(response.items()).willReturn(
      listOf(
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test1\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.ORPHANAGE.value)),
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test2\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.ORPHANAGE.value)),
      ),
    )
  }

  @Test
  fun `returns the correct product definitions`() {
    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions).isNotNull
    assertThat(productDefinitions.count()).isEqualTo(2)
    assertThat(productDefinitions[0].path).isEqualTo(DataDefinitionPath.ORPHANAGE)
    assertThat(productDefinitions[1].path).isEqualTo(DataDefinitionPath.ORPHANAGE)

    then(dynamoDbClient).should().scan(getScanRequest(properties, listOf(DataDefinitionPath.MISSING.value, DataDefinitionPath.ORPHANAGE.value)))
  }

  @Test
  fun `returns the correct product definition`() {
    val response = mock<GetItemResponse>()
    given(response.hasItem()).willReturn(true)
    given(response.item()).willReturn(
      mapOf("definition" to AttributeValue.fromS("{\"id\": \"test2\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.MISSING.value)),
    )
    given(dynamoDbClient.getItem(any(GetItemRequest::class.java))).willReturn(response)
    val productDefinition = repo.getProductDefinition("test2")

    assertThat(productDefinition).isNotNull
    assertThat(productDefinition.id).isEqualTo("test2")
    assertThat(productDefinition.path).isEqualTo(DataDefinitionPath.MISSING)
  }

  // This is to ensure that other paths are working and that the cache isn't being 'sticky' when it has data in from one set of paths already
  @Test
  fun `returns the correct product definitions using a path after first querying for the default`() {
    val path = "some/other/value"
    val response = mock<ScanResponse>()
    given(dynamoDbClient.scan(any(ScanRequest::class.java))).willReturn(response)
    given(response.items()).willReturn(
      listOf(
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test1\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.MISSING.value)),
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test2\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.ORPHANAGE.value)),
      ),
    )
    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions).isNotNull
    assertThat(productDefinitions.count()).isEqualTo(2)
    assertThat(productDefinitions[0].path).isEqualTo(DataDefinitionPath.MISSING)
      assertThat(productDefinitions[1].path).isEqualTo(DataDefinitionPath.ORPHANAGE)
    assertThat(productDefinitions[0].id).isEqualTo("test1")
    assertThat(productDefinitions[1].id).isEqualTo("test2")

    then(dynamoDbClient).should().scan(getScanRequest(properties, listOf(DataDefinitionPath.MISSING.value, DataDefinitionPath.ORPHANAGE.value)))


    given(response.items()).willReturn(
      listOf(
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test1\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.MISSING.value)),
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test3\"}"), "category" to AttributeValue.fromS(path)),
      ),
    )
    val productDefinitions2 = repo.getProductDefinitions(path)

    assertThat(productDefinitions2).isNotNull
    assertThat(productDefinitions2.count()).isEqualTo(2)
    assertThat(productDefinitions2[0].path).isEqualTo(DataDefinitionPath.MISSING)
    assertThat(productDefinitions2[1].path).isEqualTo(DataDefinitionPath.OTHER)
    assertThat(productDefinitions2[0].id).isEqualTo("test1")
    assertThat(productDefinitions2[1].id).isEqualTo("test3")
  }

  @Test
  fun `returns the correct product definition using a path`() {
    val path = DataDefinitionPath.MISSING.value
    val response = mock<GetItemResponse>()
    given(response.hasItem()).willReturn(true)
    given(response.item()).willReturn(
      mapOf("definition" to AttributeValue.fromS("{\"id\": \"test2\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.MISSING.value)),
    )
    given(dynamoDbClient.getItem(any(GetItemRequest::class.java))).willReturn(response)

    val productDefinition = repo.getProductDefinition("test2", path)

    assertThat(productDefinition).isNotNull
    assertThat(productDefinition.id).isEqualTo("test2")
    assertThat(productDefinition.path).isEqualTo(DataDefinitionPath.MISSING)
  }

  @Test
  fun `returns definitions from missing as well as main path if cache is loaded`() {
    val response = mock<ScanResponse>()
    given(dynamoDbClient.scan(any(ScanRequest::class.java))).willReturn(response)
    given(response.items()).willReturn(
      listOf(
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test1\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.ORPHANAGE.value)),
        mapOf("definition" to AttributeValue.fromS("{\"id\": \"test2\"}"), "category" to AttributeValue.fromS(DataDefinitionPath.MISSING.value)),
      ),
    )
    val productDefinitions = repo.getProductDefinitions()
    assertThat(productDefinitions).hasSize(2)

    // Run the get again, which will hit the cache
    assertThat(repo.getProductDefinitions()).hasSize(2)
  }
}
