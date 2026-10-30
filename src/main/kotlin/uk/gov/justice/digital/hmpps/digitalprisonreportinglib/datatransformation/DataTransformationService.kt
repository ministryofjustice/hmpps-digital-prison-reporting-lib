package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DataProductExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.PolicyBasedDataChange
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Service
class DataTransformationService {
  private final val dataTransformationPolicyTypesToTransformers: Map<PolicyType, KClass<out DataTransformer>> = mapOf(
    PolicyType.LAO to LaoDataTransformer::class,
  )

  fun transformResults(
    data: List<Map<String, Any?>>,
    product: SingleReportProductDefinition,
    executionContext: ExecutionContext,
  ): ReportExecutionResults =
    transformResultsImpl(data, product, executionContext) as ReportExecutionResults

  fun transformResults(
    data: List<Map<String, Any?>>,
    product: SingleDashboardProductDefinition,
    executionContext: ExecutionContext,
  ): DashboardExecutionResults =
    transformResultsImpl(data, product, executionContext) as DashboardExecutionResults

  private fun transformResultsImpl(
    data: List<Map<String, Any?>>,
    product: WithPolicy,
    executionContext: ExecutionContext,
  ): DataProductExecutionResults {
    val data = transformData(
      data,
      product,
      executionContext,
    )
    return when (product) {
      is SingleReportProductDefinition -> ReportExecutionResults(
        data = data.first,
        policyBasedDataChanges = data.second
      )
      is SingleDashboardProductDefinition -> DashboardExecutionResults(
        data = data.first,
        policyBasedDataChanges = data.second,
      )
    }
  }

  private fun transformData(
    data: List<Map<String, Any?>>,
    product: WithPolicy,
    executionContext: ExecutionContext,
  ): Pair<List<Map<String, Any?>>, Map<PolicyType, List<PolicyBasedDataChange>>> {
    val transformationPolicies = product.policy.filter { it.type in dataTransformationPolicyTypesToTransformers.keys }
    if (transformationPolicies.isEmpty()) {
      return Pair(data, emptyMap())
    }

    val duplicates = transformationPolicies.groupBy(Policy::type).filterValues { it.size > 1 }

    // At present there shouldn't be multiple of the same type of transformation policy
    if (duplicates.isNotEmpty()) {
      throw ValidationException("Multiple duplicate policies found for type(s) ${duplicates.keys}")
    }

    var currentData = data
    val allChanges = mutableMapOf<PolicyType, List<PolicyBasedDataChange>>()

    for (policy in transformationPolicies) {
      val transformer = dataTransformationPolicyTypesToTransformers[policy.type]?.createInstance()
        ?: error("No transformer for ${policy.type}")

      val (newData, changes) = transformer.transform(currentData, policy, executionContext)

      currentData = newData
      allChanges[policy.type] = changes
    }

    return Pair(data, allChanges)
  }
}
