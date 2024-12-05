package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Component
class ProductDefinitionTokenPolicyChecker {
  fun determineAuth(
    withPolicy: WithPolicy,
    userToken: DprAuthAwareAuthenticationToken?,
  ): Boolean {
    val policyEngine = PolicyEngine(withPolicy.policy, userToken)
    val result = policyEngine.execute()
    return if (result == Policy.PolicyResult.POLICY_PERMIT) true else false
  }
}
