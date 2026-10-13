package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.CacheBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import org.mockito.kotlin.times
import software.amazon.awssdk.core.pagination.sync.SdkIterable
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
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

  @Test
  fun `returns the correct product definitions`() {
    val orphanageItems = listOf(
      mapOf(
        "definition" to AttributeValue.fromS("""{"id": "test1"}"""),
        "category" to AttributeValue.fromS(DataDefinitionPath.ORPHANAGE.value),
      ),
    )
    val orphanagePaginator = mock<QueryIterable>()

    given(orphanagePaginator.items()).willReturn(SdkIterable { orphanageItems.toMutableList().iterator() })
    given(dynamoDbClient.queryPaginator(any<QueryRequest>())).willAnswer { invocation ->
      val request = invocation.getArgument<QueryRequest>(0)
      when (val category = request.expressionAttributeValues()[":category"]?.s()) {
        DataDefinitionPath.ORPHANAGE.value -> orphanagePaginator
        else -> throw IllegalArgumentException("Unexpected category: $category")
      }
    }
    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions).isNotNull
    assertThat(productDefinitions.count()).isEqualTo(1)
    assertThat(productDefinitions[0].path).isEqualTo(DataDefinitionPath.ORPHANAGE)

    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())
  }

  @Test
  fun `returns the correct product definition`() {
    val response = mock<GetItemResponse>()
    given(response.hasItem()).willReturn(true)
    given(response.item()).willReturn(
      mapOf(
        "definition" to AttributeValue.fromS("{\"id\": \"test2\"}"),
        "category" to AttributeValue.fromS(DataDefinitionPath.MISSING.value),
      ),
    )
    given(dynamoDbClient.getItem(any(GetItemRequest::class.java))).willReturn(response)
    val productDefinition = repo.getProductDefinition("test2")

    assertThat(productDefinition).isNotNull
    assertThat(productDefinition.id).isEqualTo("test2")
    assertThat(productDefinition.path).isEqualTo(DataDefinitionPath.MISSING)
  }
}
