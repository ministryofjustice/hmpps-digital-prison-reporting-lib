package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import kotlin.reflect.KClass

interface DataTransformer {
  val policyType: PolicyType

  fun transform(data: List<Map<String, Any?>>, policies: List<Policy>, executionContext: ExecutionContext, clazz: KClass<out WithPolicy>): DataTransformation
}
