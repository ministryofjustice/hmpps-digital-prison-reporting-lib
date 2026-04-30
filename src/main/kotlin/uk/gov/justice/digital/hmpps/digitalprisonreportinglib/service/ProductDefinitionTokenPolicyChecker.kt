package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Component
class ProductDefinitionTokenPolicyChecker {
  fun determineAuth(
    withPolicy: WithPolicy,
    authToken: DprAuthAwareAuthenticationToken?,
  ): Boolean {
    val policyEngine = PolicyEngine(withPolicy.policy, authToken)
    val result = policyEngine.execute(PolicyType.ACCESS)
    return result == Policy.PolicyResult.POLICY_PERMIT
  }
}
