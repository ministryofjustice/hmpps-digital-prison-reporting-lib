package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.common.cache.CacheBuilder
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.then
import org.mockito.kotlin.times
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.pagination.sync.SdkIterable
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CommonPrefix
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.AwsProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.LoadedDefinitions
import java.util.concurrent.TimeUnit

class S3AndDynamoDbProductDefinitionRepositoryTest {

  private val dynamoDbClient = mock<DynamoDbClient>()
  private val s3Client = mock<S3Client>()

  private val properties = AwsProperties(
    dynamoDb = AwsProperties.DynamoDb(),
    sts = AwsProperties.Sts(),
  )

  private val repo = S3AndDynamoDbProductDefinitionRepository(
    dynamoDbClient = dynamoDbClient,
    s3Client = s3Client,
    gson = DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    properties = properties,
    s3Bucket = "dpr-data-product-definitions-development",
    identifiedHelper = IdentifiedHelper(),
    s3AndDdbDefinitionsCache = CacheBuilder.newBuilder()
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .concurrencyLevel(Runtime.getRuntime().availableProcessors())
      .build<String, LoadedDefinitions>(),
  )

  @Test
  fun `returns combined DDB orphanage and S3 product definitions with prefixed IDs`() {
    givenDynamoDbOrphanageDefinitions(
      listOf(
        definitionJson("legacy-report", "Legacy Report"),
      ),
    )

    givenS3BucketContents(
      teamPrefixes = listOf("activities", "incidents"),
      teamPrefixToS3Keys = mapOf(
        "activities" to listOf(
          "activities/activity-report.json",
          "activities/subfolder/nested-activity-report.json",
        ),
        "incidents" to listOf(
          "incidents/incident-report.json",
        ),
      ),
    )

    givenS3Objects(
      mapOf(
        "activities/activity-report.json" to definitionJson("activity-report", "Activity Report"),
        "activities/subfolder/nested-activity-report.json" to definitionJson("nested-activity-report", "Nested Activity Report"),
        "incidents/incident-report.json" to definitionJson("incident-report", "Incident Report"),
      ),
    )

    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions).hasSize(4)

    assertThat(productDefinitions.map { it.id }).containsExactlyInAnyOrder(
      "dpr_legacy-report",
      "activities_activity-report",
      "activities_nested-activity-report",
      "incidents_incident-report",
    )

    assertThat(productDefinitions).allSatisfy {
      assertThat(it.path).isNull()
    }
    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())

    // One call to list top-level team prefixes and one list call per team
    then(s3Client).should(times(3)).listObjectsV2Paginator(any<ListObjectsV2Request>())

    then(s3Client).should(times(3)).getObject(
      any<GetObjectRequest>(),
      any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
    )
  }

  @Test
  fun `returns full S3 product definition by prefixed ID`() {
    givenDynamoDbOrphanageDefinitions(emptyList())

    givenS3BucketContents(
      teamPrefixes = listOf("activities"),
      teamPrefixToS3Keys = mapOf(
        "activities" to listOf("activities/activity-report.json"),
      ),
    )

    givenS3Objects(
      mapOf(
        "activities/activity-report.json" to definitionJson("activity-report", "Activity Report"),
      ),
    )

    val productDefinition = repo.getProductDefinition("activities_activity-report")

    assertThat(productDefinition).isNotNull
    assertThat(productDefinition.id).isEqualTo("activities_activity-report")
    assertThat(productDefinition.name).isEqualTo("Activity Report")
    assertThat(productDefinition.path).isNull()

    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())
    then(s3Client).should(times(2)).listObjectsV2Paginator(any<ListObjectsV2Request>())
    then(s3Client).should(times(1)).getObject(
      any<GetObjectRequest>(),
      any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
    )
  }

  @Test
  fun `returns full DDB product definition by dpr prefixed ID`() {
    givenDynamoDbOrphanageDefinitions(
      listOf(
        definitionJson("legacy-report", "Legacy Report"),
      ),
    )

    givenS3BucketContents(
      teamPrefixes = emptyList(),
      teamPrefixToS3Keys = emptyMap(),
    )

    val productDefinition = repo.getProductDefinition("dpr_legacy-report")

    assertThat(productDefinition).isNotNull
    assertThat(productDefinition.id).isEqualTo("dpr_legacy-report")
    assertThat(productDefinition.name).isEqualTo("Legacy Report")
    assertThat(productDefinition.path).isNull()

    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())
    then(s3Client).should(times(1)).listObjectsV2Paginator(any<ListObjectsV2Request>())
    then(s3Client).should(times(0)).getObject(
      any<GetObjectRequest>(),
      any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
    )
  }

  @Test
  fun `skips invalid S3 JSON files and returns valid definitions`() {
    givenDynamoDbOrphanageDefinitions(
      listOf(
        definitionJson("legacy-report", "Legacy Report"),
      ),
    )

    givenS3BucketContents(
      teamPrefixes = listOf("activities"),
      teamPrefixToS3Keys = mapOf(
        "activities" to listOf(
          "activities/valid-report.json",
          "activities/invalid-report.json",
        ),
      ),
    )

    givenS3Objects(
      mapOf(
        "activities/valid-report.json" to definitionJson("valid-report", "Valid Report"),
        "activities/invalid-report.json" to """{"id": """,
      ),
    )

    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions).hasSize(2)

    assertThat(productDefinitions.map { it.id }).containsExactlyInAnyOrder(
      "dpr_legacy-report",
      "activities_valid-report",
    )

    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())
    then(s3Client).should(times(2)).listObjectsV2Paginator(any<ListObjectsV2Request>())

    // Both objects are fetched; one is skipped after JSON parsing fails.
    then(s3Client).should(times(2)).getObject(
      any<GetObjectRequest>(),
      any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
    )
  }

  @Test
  fun `throws ValidationException when product definition ID does not exist`() {
    givenDynamoDbOrphanageDefinitions(emptyList())

    givenS3BucketContents(
      teamPrefixes = emptyList(),
      teamPrefixToS3Keys = emptyMap(),
    )

    assertThatThrownBy {
      repo.getProductDefinition("unknown-report")
    }
      .isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("unknown-report")

    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())
    then(s3Client).should(times(1)).listObjectsV2Paginator(any<ListObjectsV2Request>())
  }

  @Test
  fun `uses cache for repeated calls`() {
    givenDynamoDbOrphanageDefinitions(
      listOf(
        definitionJson("legacy-report", "Legacy Report"),
      ),
    )

    givenS3BucketContents(
      teamPrefixes = listOf("activities"),
      teamPrefixToS3Keys = mapOf(
        "activities" to listOf("activities/activity-report.json"),
      ),
    )

    givenS3Objects(
      mapOf(
        "activities/activity-report.json" to definitionJson("activity-report", "Activity Report"),
      ),
    )

    val firstCall = repo.getProductDefinitions()
    val secondCall = repo.getProductDefinitions()

    assertThat(firstCall).hasSize(2)
    assertThat(secondCall).hasSize(2)

    assertThat(firstCall.map { it.id }).containsExactlyInAnyOrder(
      "dpr_legacy-report",
      "activities_activity-report",
    )

    assertThat(secondCall.map { it.id }).containsExactlyInAnyOrder(
      "dpr_legacy-report",
      "activities_activity-report",
    )

    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())

    // one call to list team prefixes and one call to list keys for activities.
    then(s3Client).should(times(2)).listObjectsV2Paginator(any<ListObjectsV2Request>())

    then(s3Client).should(times(1)).getObject(
      any<GetObjectRequest>(),
      any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
    )
  }

  @Test
  fun `reads json files in nested S3 folders under team prefix`() {
    givenDynamoDbOrphanageDefinitions(emptyList())

    givenS3BucketContents(
      teamPrefixes = listOf("activities"),
      teamPrefixToS3Keys = mapOf(
        "activities" to listOf(
          "activities/report-directly-under-team.json",
          "activities/nested/report-in-subfolder.json",
          "activities/nested/deeper/report-in-deeper-subfolder.json",
        ),
      ),
    )

    givenS3Objects(
      mapOf(
        "activities/report-directly-under-team.json" to definitionJson("direct-report", "Direct Report"),
        "activities/nested/report-in-subfolder.json" to definitionJson("nested-report", "Nested Report"),
        "activities/nested/deeper/report-in-deeper-subfolder.json" to definitionJson("deeper-nested-report", "Deeper Nested Report"),
      ),
    )

    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions.map { it.id }).containsExactlyInAnyOrder(
      "activities_direct-report",
      "activities_nested-report",
      "activities_deeper-nested-report",
    )

    then(s3Client).should(times(3)).getObject(
      any<GetObjectRequest>(),
      any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
    )
  }

  @Test
  fun `skips DDB and S3 definitions when required fields are null and returns only valid definitions`() {
    givenDynamoDbOrphanageDefinitions(
      listOf(
        definitionJson("valid-ddb-report", "Valid DDB Report"),
        definitionJsonWithNullField("invalid-ddb-report", "name"),
      ),
    )

    givenS3BucketContents(
      teamPrefixes = listOf("activities"),
      teamPrefixToS3Keys = mapOf(
        "activities" to listOf(
          "activities/valid-s3-report.json",
          "activities/invalid-s3-report.json",
        ),
      ),
    )

    givenS3Objects(
      mapOf(
        "activities/valid-s3-report.json" to definitionJson("valid-s3-report", "Valid S3 Report"),
        "activities/invalid-s3-report.json" to definitionJsonWithNullField("invalid-s3-report", "name"),
      ),
    )

    val productDefinitions = repo.getProductDefinitions()

    assertThat(productDefinitions).hasSize(2)

    assertThat(productDefinitions.map { it.id }).containsExactlyInAnyOrder(
      "dpr_valid-ddb-report",
      "activities_valid-s3-report",
    )

    assertThat(productDefinitions).allSatisfy {
      assertThat(it.path).isNull()
    }

    then(dynamoDbClient).should(times(1)).queryPaginator(any<QueryRequest>())

    then(s3Client).should(times(2)).listObjectsV2Paginator(any<ListObjectsV2Request>())

    // both valid and invalid S3 objects are fetched but one is skipped after deserialisation failure (during copy call)
    then(s3Client).should(times(2)).getObject(
      any<GetObjectRequest>(),
      any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
    )
  }

  private fun givenDynamoDbOrphanageDefinitions(jsonDefinitions: List<String>) {
    val items = jsonDefinitions.map { json ->
      mapOf(
        properties.dynamoDb.definitionFieldName to AttributeValue.fromS(json),
        properties.dynamoDb.categoryFieldName to AttributeValue.fromS(DataDefinitionPath.ORPHANAGE.value),
      )
    }

    val paginator = mock<QueryIterable>()

    given(paginator.items()).willReturn(
      SdkIterable {
        items.toMutableList().iterator()
      },
    )

    given(dynamoDbClient.queryPaginator(any<QueryRequest>())).willAnswer { invocation ->
      val request = invocation.getArgument<QueryRequest>(0)
      val category = request.expressionAttributeValues()[":category"]?.s()

      if (category == DataDefinitionPath.ORPHANAGE.value) {
        paginator
      } else {
        throw IllegalArgumentException("Unexpected category: $category")
      }
    }
  }

  private fun givenS3BucketContents(teamPrefixes: List<String>, teamPrefixToS3Keys: Map<String, List<String>>) {
    val commonPrefixes = teamPrefixes.map {
      CommonPrefix.builder()
        .prefix("$it/")
        .build()
    }

    val teamPrefixResponse = ListObjectsV2Response.builder()
      .commonPrefixes(commonPrefixes)
      .build()

    val teamPrefixToKeyResponses = teamPrefixToS3Keys.mapValues { (_, keys) ->
      ListObjectsV2Response.builder()
        .contents(
          keys.map {
            S3Object.builder()
              .key(it)
              .build()
          },
        )
        .build()
    }

    given(s3Client.listObjectsV2Paginator(any<ListObjectsV2Request>())).willAnswer { invocation ->
      val request = invocation.getArgument<ListObjectsV2Request?>(0)
        ?: throw IllegalArgumentException("ListObjectsV2Request must not be null")

      when {
        request.delimiter() == "/" -> paginatorOf(teamPrefixResponse)

        request.prefix() != null -> {
          val teamPrefix = request.prefix().removeSuffix("/")

          paginatorOf(
            teamPrefixToKeyResponses[teamPrefix]
              ?: throw IllegalArgumentException("Unexpected S3 team prefix: ${request.prefix()}"),
          )
        }

        else -> throw IllegalArgumentException("Unexpected S3 list request: $request")
      }
    }
  }

  private fun paginatorOf(response: ListObjectsV2Response): ListObjectsV2Iterable {
    val paginator = mock<ListObjectsV2Iterable>()

    given(paginator.iterator()).willAnswer {
      mutableListOf(response).iterator()
    }

    given(paginator.contents()).willReturn(
      SdkIterable {
        response.contents().toMutableList().iterator()
      },
    )

    given(paginator.commonPrefixes()).willReturn(
      SdkIterable {
        response.commonPrefixes().toMutableList().iterator()
      },
    )

    return paginator
  }

  private fun givenS3Objects(s3KeyToObject: Map<String, String>) {
    given(
      s3Client.getObject(
        any<GetObjectRequest>(),
        any<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>(),
      ),
    ).willAnswer { invocation ->
      val request = invocation.getArgument<GetObjectRequest>(0)
      val json = s3KeyToObject[request.key()]
        ?: throw IllegalArgumentException("Unexpected S3 key: ${request.key()}")

      ResponseBytes.fromByteArray(
        GetObjectResponse.builder().build(),
        json.toByteArray(Charsets.UTF_8),
      )
    }
  }

  private fun definitionJson(id: String, name: String = id): String {
    val resource = this::class.java.classLoader.getResource("productDefinition.json")

    val jsonObject = JsonParser.parseString(resource!!.readText()).asJsonObject
    jsonObject.addProperty("id", id)
    jsonObject.addProperty("name", name)

    return jsonObject.toString()
  }

  private fun definitionJsonWithNullField(id: String, fieldName: String): String {
    val resource = this::class.java.classLoader.getResource("productDefinition.json")

    val jsonObject = JsonParser.parseString(resource!!.readText()).asJsonObject
    jsonObject.addProperty("id", id)
    jsonObject.add(fieldName, JsonNull.INSTANCE)

    return jsonObject.toString()
  }
}
