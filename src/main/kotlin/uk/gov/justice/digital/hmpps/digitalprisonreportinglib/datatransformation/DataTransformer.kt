package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.PolicyBasedDataChange
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy

interface DataTransformer {
  fun transform(data: List<Map<String, Any?>>, policy: Policy, executionContext: ExecutionContext): Pair<List<Map<String, Any?>>, List<PolicyBasedDataChange>>
}