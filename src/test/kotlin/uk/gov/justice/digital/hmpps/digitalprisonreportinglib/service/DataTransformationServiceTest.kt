package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestFlywayConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ColumnChange
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.LaoPolicyMetadata
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.PolicyBasedDataChange
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RowIdentifier
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation.DataTransformationService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation.LaoData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation.LaoDataRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestFlywayConfig::class)
@AutoConfigureWebTestClient
class DataTransformationServiceTest {
  @Autowired
  private lateinit var dataTransformationService: DataTransformationService

  @MockitoBean(name = "athenaApiRepository")
  lateinit var athenaApiRepository: AthenaApiRepository

  @MockitoBean
  lateinit var asyncDataApiService: AsyncDataApiService

  @MockitoSpyBean
  private lateinit var laoDataRepository: LaoDataRepository

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }

    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun setupClass(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
      registry.add("spring.datasource.url", { PostgresContainer.jdbcUrl })
      registry.add("spring.datasource.username", { PostgresContainer.username })
      registry.add("spring.datasource.password", { PostgresContainer.password })
    }
  }

  @Test
  fun `can transform data with an lao policy`() {
    val reportDpd = mock<SingleReportProductDefinition>()
    val executionContext = mock<ExecutionContext>()
    val policy = Policy("lao", PolicyType.LAO, listOf("bar"), listOf(Rule(Effect.DENY, emptyList())))
    whenever(reportDpd.policy).thenReturn(listOf(policy))
    whenever(executionContext.userInfo).thenReturn(AuthUser("foo", true, "bar", AuthSource.DELIUS, "id", "uuid"))
    whenever(laoDataRepository.getLaoExclusionsForUser("foo")).thenReturn(
      listOf(
        LaoData("bing", "EXCLUSION", "foo", null, ZonedDateTime.now(), ZonedDateTime.now().plusDays(1)),
      ),
    )
    whenever(laoDataRepository.getLaoRestrictionsForCrns(listOf("baz", "bing", "boom"))).thenReturn(
      listOf(
        LaoData("baz", "RESTRICTION", "fee", null, ZonedDateTime.now(), ZonedDateTime.now().plusDays(1)),
      ),
    )
    val results = dataTransformationService.transformResults(
      listOf(
        mapOf("foo" to "bar", "bar" to "baz"),
        mapOf("foo" to "bar", "bar" to "bing"),
        mapOf("foo" to "bar", "bar" to "boom"),
      ),
      reportDpd,
      executionContext,
    )
    assertThat(results.data).hasSize(1)
    assertThat(results.data).containsExactlyInAnyOrder(mapOf("foo" to "bar", "bar" to "boom"))
    assertThat(results.policyBasedDataChanges.keys).hasSize(1)
    assertThat(results.policyBasedDataChanges[PolicyType.LAO]).hasSize(2)
    assertThat(results.policyBasedDataChanges[PolicyType.LAO]).containsExactlyInAnyOrder(
      PolicyBasedDataChange(
        rowIdentifier = RowIdentifier(rowIndex = 1L),
        columnChanges = mapOf("foo" to "bar", "bar" to "baz").keys.map { colName -> ColumnChange(columnName = colName, newColumnValue = "") },
        policy = policy,
        policySpecificMetaData = LaoPolicyMetadata(
          identifyingColumn = "bar",
          identifyingColumnValue = "baz",
        ),
      ),
      PolicyBasedDataChange(
        rowIdentifier = RowIdentifier(rowIndex = 1L),
        columnChanges = mapOf("foo" to "bar", "bar" to "bing").keys.map { colName -> ColumnChange(columnName = colName, newColumnValue = "") },
        policy = policy,
        policySpecificMetaData = LaoPolicyMetadata(
          identifyingColumn = "bar",
          identifyingColumnValue = "bing",
        ),
      ),
    )
  }
}
