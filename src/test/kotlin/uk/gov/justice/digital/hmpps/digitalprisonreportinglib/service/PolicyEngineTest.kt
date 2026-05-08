package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.set
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Condition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType.ACCESS
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType.ROW_LEVEL
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class PolicyEngineTest {

  val testUsername = "request-user"
  val testCaseload = Caseload("ABC", "ABC")

  @Test
  fun `policy engine permits given action for an active caseload`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = testCaseload,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code='\${caseload}' AND lower(direction)='out') OR (destination_code='\${caseload}' AND lower(direction)='in')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    val expected = "(origin_code='ABC' AND lower(direction)='out') OR (destination_code='ABC' AND lower(direction)='in')"
    assertThat(policyEngine.execute()).isEqualTo(expected)
  }

  @Test
  fun `policy engine denies given action for no policies`() {
    val policyEngine = PolicyEngine(emptyList())
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine denies given action for no caseload with no conditions`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine denies given action for no caseload with exists conditions`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${caseload}"))))),
    )
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine permits a policy with a permit rule with no conditions and an action of TRUE`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_PERMIT)
  }

  @Test
  fun `policy engine denies a policy with a permit rule with no conditions and an action of FALSE`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(testCaseload),
          activeCaseload = testCaseload,
        ),
        listOf("A_ROLE"),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("FALSE"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine returns FALSE for a policy with a deny rule with no conditions and an action of TRUE`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(testCaseload),
          activeCaseload = testCaseload,
        ),
        listOf("A_ROLE"),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf(PolicyResult.POLICY_PERMIT),
      listOf(Rule(Effect.DENY, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine returns TRUE for a policy with a permit rule with a condition that a token exists and an action of TRUE when there is a token`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${token}"))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_PERMIT)
  }

  @Test
  fun `policy engine returns FALSE for a policy with a permit rule with a condition that a token exists when the token is invalid`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = "",
          active = false,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        emptyList(),
        AuthUser("", false, "", AuthSource.NONE, "", ""),
      ).set()
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${token}"))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine returns TRUE for a policy with a permit rule with a condition of matching a role and an action of TRUE when there is a matching role`() {
    val userRole = "A_ROLE"
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        listOf(userRole),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()

    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", userRole))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_PERMIT)
  }

  @Test
  fun `policy engine returns FALSE for a policy with a permit rule with a condition of matching a role and an action of TRUE when there is no matching role`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(testCaseload),
          activeCaseload = testCaseload,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", "B_ROLE"))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine returns FALSE when one of the policies is denied`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(testCaseload),
          activeCaseload = testCaseload,
        ),
        listOf("A_ROLE"),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
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
    val policyEngine = PolicyEngine(listOf(policy1, policy2))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
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
    val policy3 = Policy(
      "caseloads",
      ROW_LEVEL,
      listOf("origin_code in (\${caseloads})"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )

    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(
            testCaseload,
            Caseload("BBC", "BBC"),
            Caseload("HEI", "HEI"),
            Caseload("MDI", "MDI"),
          ),
          activeCaseload = testCaseload,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policyEngine = PolicyEngine(listOf(policy1, policy2, policy3))
    val expected = "(origin_code='ABC' AND lower(direction)='out') OR (destination_code='ABC' AND lower(direction)='in') AND ${PolicyResult.POLICY_PERMIT} AND origin_code in ('ABC', 'BBC', 'HEI', 'MDI')"
    assertThat(policyEngine.execute()).isEqualTo(expected)
  }

  @Test
  fun `policy engine permits an access policy with a permit rule with with an empty condition`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(testCaseload),
          activeCaseload = testCaseload,
        ),
        listOf("A_ROLE"),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      id = "caseload",
      type = ACCESS,
      rule = listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_PERMIT)
  }

  @Test
  fun `policy engine denies an access policy with a deny rule with with an empty condition`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(testCaseload),
          activeCaseload = testCaseload,
        ),
        listOf("A_ROLE"),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      id = "caseload",
      type = ACCESS,
      rule = listOf(Rule(Effect.DENY, emptyList())),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }

  @Test
  fun `policy engine returns TRUE for an access policy with a permit rule with a condition that a token exists when there is a token`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      id = "caseload",
      type = ACCESS,
      rule = listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${token}"))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_PERMIT)
  }

  @Test
  fun `policy engine returns TRUE for an access policy with a permit rule with a condition of matching a role when there is one matching role`() {
    val userRole = "DPR-USER"
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        listOf(userRole),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      id = "caseload",
      type = ACCESS,
      rule = listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", userRole))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_PERMIT)
  }

  @Test
  fun `policy engine returns TRUE for an access policy with a permit rule with a condition of matching a role when any role matches`() {
    val userRole = "DPR-USER"
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = emptyList(),
          activeCaseload = null,
        ),
        listOf(userRole),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      id = "caseload",
      type = ACCESS,
      rule = listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", userRole, "RANDOM-ROLE", "GLOBAL-SEARCH"))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_PERMIT)
  }

  @Test
  fun `policy engine returns FALSE for an access policy with a permit rule with a condition of matching a role when no role matches`() {
    ExecutionContext
      .get()
      .copy(
        CaseloadResponse(
          username = testUsername,
          active = true,
          accountType = "GENERAL",
          caseloads = listOf(testCaseload),
          activeCaseload = testCaseload,
        ),
        emptyList(),
        AuthUser(testUsername, true, testUsername, AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
      ).set()
    val policy = Policy(
      id = "caseload",
      type = ACCESS,
      rule = listOf(Rule(Effect.PERMIT, listOf(Condition(match = listOf("\${role}", "DPR-USER", "RANDOM-ROLE", "GLOBAL-SEARCH"))))),
    )
    val policyEngine = PolicyEngine(listOf(policy))
    assertThat(policyEngine.execute()).isEqualTo(PolicyResult.POLICY_DENY)
  }
}
