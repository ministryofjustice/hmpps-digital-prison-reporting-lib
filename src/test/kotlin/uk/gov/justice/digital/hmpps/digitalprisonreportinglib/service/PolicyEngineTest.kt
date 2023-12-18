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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

class PolicyEngineTest {

  private val authToken = mock<DprAuthAwareAuthenticationToken>()

  @Test
  fun `policy engine permits given action for an active caseload`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code='\${caseload}' AND lower(direction)='out') OR (destination_code='\${caseload}' AND lower(direction)='in')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    whenever(authToken.getCaseLoads()).thenReturn(listOf("ABC"))
    val policyEngine = PolicyEngine(listOf(policy), authToken)
    val expected = "(origin_code='ABC' AND lower(direction)='out') OR (destination_code='ABC' AND lower(direction)='in')"
    Assertions.assertThat(policyEngine.execute()).isEqualTo(expected)
  }

  @Test
  fun `policy engine denies given action for no policies`() {
    val policyEngine = PolicyEngine(emptyList())
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine denies given action for no auth token`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine denies given action for no caseload with no conditions`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    whenever(authToken.getCaseLoads()).thenReturn(emptyList())
    val policyEngine = PolicyEngine(listOf(policy), authToken)
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
    whenever(authToken.getCaseLoads()).thenReturn(emptyList())
    val policyEngine = PolicyEngine(listOf(policy), authToken)
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
    val policyEngine = PolicyEngine(listOf(policy))
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
    val policyEngine = PolicyEngine(listOf(policy))
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
    val policyEngine = PolicyEngine(listOf(policy))
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
    val policyEngine = PolicyEngine(listOf(policy), authToken = authToken)
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
    val policyEngine = PolicyEngine(listOf(policy), authToken = null)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine returns TRUE for a policy with a permit rule with a condition of matching a role and an action of TRUE when there is a matching role`() {
    val userRole = "A_ROLE"
    val authToken = mock<DprAuthAwareAuthenticationToken>()
    whenever(authToken.authorities).thenReturn(listOf(SimpleGrantedAuthority(userRole)))
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", userRole))))),
    )
    val policyEngine = PolicyEngine(listOf(policy), authToken = authToken)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("TRUE")
  }

  @Test
  fun `policy engine returns FALSE for a policy with a permit rule with a condition of matching a role and an action of TRUE when there is no matching role`() {
    val authToken = mock<DprAuthAwareAuthenticationToken>()
    whenever(authToken.authorities).thenReturn(listOf(SimpleGrantedAuthority("A_ROLE")))
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", "B_ROLE"))))),
    )
    val policyEngine = PolicyEngine(listOf(policy), authToken = authToken)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine returns FALSE when one of the policies is denied`() {
    val authToken = mock<DprAuthAwareAuthenticationToken>()
    whenever(authToken.authorities).thenReturn(listOf(SimpleGrantedAuthority("A_ROLE")))
    val policy1 = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", "A_ROLE"))))),
    )
    val policy2 = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("FALSE"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy1, policy2), authToken = authToken)
    Assertions.assertThat(policyEngine.execute()).isEqualTo("FALSE")
  }

  @Test
  fun `policy engine returns all the permitted results of the list of policies when there is no denial`() {
    val policy1 = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code='\${caseload}' AND lower(direction)='out') OR (destination_code='\${caseload}' AND lower(direction)='in')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policy2 = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${token}"))))),
    )
    whenever(authToken.getCaseLoads()).thenReturn(listOf("ABC"))
    val policyEngine = PolicyEngine(listOf(policy1, policy2), authToken)
    val expected = "(origin_code='ABC' AND lower(direction)='out') OR (destination_code='ABC' AND lower(direction)='in') AND TRUE"
    Assertions.assertThat(policyEngine.execute()).isEqualTo(expected)
  }
}
