package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Condition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType.ROW_LEVEL
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.AuthAwareAuthenticationToken

class PolicyEngineTest {

  private val authToken = mock<AuthAwareAuthenticationToken>()

  @Test
  fun `policy engine permits given action for an active caseload`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    whenever(authToken.getCaseLoads()).thenReturn(listOf("ABC"))
    val policyEngine = PolicyEngine(policy, authToken)
    val expected = "(origin_code='ABC' AND direction='OUT') OR (destination_code='ABC' AND direction='IN')"
    Assertions.assertThat(policyEngine.execute()).isEqualTo(expected)
  }

  @Test
  fun `policy engine denies given action for no caseload with no conditions`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(policy)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine denies given action for no caseload with exists conditions`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${caseload}"))))),
    )
    val policyEngine = PolicyEngine(policy)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine permits a policy with a permit rule with no conditions and an action of TRUE`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(policy)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("TRUE")
  }

  @Test
  fun `policy engine returns FALSE for a policy with a permit rule with no conditions and an action of FALSE`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("FALSE"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(policy)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine returns FALSE for a policy with a deny rule with no conditions and an action of TRUE`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.DENY, emptyList())),
    )
    val policyEngine = PolicyEngine(policy)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine returns TRUE for a policy with a permit rule with a condition that a token exists and an action of TRUE when there is a token`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${token}"))))),
    )
    val policyEngine = PolicyEngine(policy, authToken = authToken)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("TRUE")
  }

  @Test
  fun `policy engine returns FALSE for a policy with a permit rule with a condition that a token exists and an action of TRUE when the token is null`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${token}"))))),
    )
    val policyEngine = PolicyEngine(policy, authToken = null)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine returns TRUE for a policy with a permit rule with a condition of matching a role and an action of TRUE when there is a matching role`() {
    val userRole = "A_ROLE"
    val authToken = mock<AuthAwareAuthenticationToken>()
    whenever(authToken.authorities).thenReturn(listOf(SimpleGrantedAuthority(userRole)))
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", userRole))))),
    )
    val policyEngine = PolicyEngine(policy, authToken = authToken)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("TRUE")
  }

  @Test
  fun `policy engine returns FALSE for a policy with a permit rule with a condition of matching a role and an action of TRUE when there is no matching role`() {
    val authToken = mock<AuthAwareAuthenticationToken>()
    whenever(authToken.authorities).thenReturn(listOf(SimpleGrantedAuthority("A_ROLE")))
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", "B_ROLE"))))),
    )
    val policyEngine = PolicyEngine(policy, authToken = authToken)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }
}
