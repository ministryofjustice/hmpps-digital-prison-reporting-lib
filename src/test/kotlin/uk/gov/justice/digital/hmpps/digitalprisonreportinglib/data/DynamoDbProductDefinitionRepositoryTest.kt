package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.QueryResponse
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DynamoDbProductDefinitionProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DynamoDbProductDefinitionRepository.Companion.defaultPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DynamoDbProductDefinitionRepository.Companion.getQueryRequest

class DynamoDbProductDefinitionRepositoryTest {

  private val dynamoDbClient = mock<DynamoDbClient>()
  private val properties = DynamoDbProductDefinitionProperties()

  private val repo = DynamoDbProductDefinitionRepository(
    dynamoDbClient = dynamoDbClient,
    gson = DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    properties = properties
  )

  @BeforeEach
  fun setup(): Unit = runBlocking {
    val response = mock<QueryResponse>()

    given(dynamoDbClient.query(any())).willReturn(response)
    given(response.items).willReturn(listOf(
      mapOf("definition" to AttributeValue.S("{\"id\": \"test1\"}")),
      mapOf("definition" to AttributeValue.S("{\"id\": \"test2\"}"))
    ))
  }

  @Test
  fun `returns the correct product definitions`(): Unit = runBlocking {
    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions).isNotNull
    assertThat(productDefinitions.count()).isEqualTo(2)

    then(dynamoDbClient).should().query(getQueryRequest(properties, defaultPath))
  }

  @Test
  fun `returns the correct product definition`(): Unit = runBlocking {
    val productDefinition = repo.getProductDefinition("test2")

    assertThat(productDefinition).isNotNull
    assertThat(productDefinition.id).isEqualTo("test2")
    then(dynamoDbClient).should().query(getQueryRequest(properties, defaultPath))
  }

  @Test
  fun `returns the correct product definitions using a path`(): Unit = runBlocking {
    val path = "dpd/path"

    val productDefinitions = repo.getProductDefinitions(path)

    assertThat(productDefinitions).isNotNull
    assertThat(productDefinitions.count()).isEqualTo(2)
    then(dynamoDbClient).should().query(getQueryRequest(properties, path))
  }

  @Test
  fun `returns the correct product definition using a path`(): Unit = runBlocking {
    val path = "dpd/path"

    val productDefinition = repo.getProductDefinition("test2", path)

    assertThat(productDefinition).isNotNull
    assertThat(productDefinition.id).isEqualTo("test2")
    then(dynamoDbClient).should().query(getQueryRequest(properties, path))
  }
}
