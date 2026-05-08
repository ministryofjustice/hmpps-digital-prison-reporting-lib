package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy

@Component
class ProductDefinitionTokenPolicyChecker {
  fun determineAuth(
    withPolicy: WithPolicy,
  ): Boolean {
    val policyEngine = PolicyEngine(withPolicy.policy)
    val result = policyEngine.execute(PolicyType.ACCESS)
    return result == Policy.PolicyResult.POLICY_PERMIT
  }
}
