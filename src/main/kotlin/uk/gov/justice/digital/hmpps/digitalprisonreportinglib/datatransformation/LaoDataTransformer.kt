package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation

import jakarta.validation.ValidationException
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ColumnChange
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.LaoPolicyMetadata
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.PolicyBasedDataChange
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RowIdentifier
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import kotlin.reflect.KClass

data class DataTransformation(
  val data: List<Map<String, Any?>>,
  val policyBasedChanges: List<PolicyBasedDataChange>,
)

@Component
class LaoDataTransformer(
  private val laoDataRepository: LaoDataRepository,
) : DataTransformer {
  override val policyType: PolicyType = PolicyType.LAO

  override fun transform(
    data: List<Map<String, Any?>>,
    policies: List<Policy>,
    executionContext: ExecutionContext,
    clazz: KClass<out WithPolicy>,
  ): DataTransformation {
    if (policies.any { it.type != policyType }) {
      throw ValidationException("LaoDataTransformer was given non LAO policies")
    }
    if (policies.isEmpty()) {
      throw ValidationException("LaoDataTransformer was given no policies")
    }
    if (policies.size > 1) {
      throw ValidationException("Data product had more than one policy of $policyType which is not currently supported")
    }

    val policy = policies.first()
    if (policy.rule.first().effect == Effect.PERMIT) {
      return DataTransformation(data, emptyList())
    }
    val userEnteredCrnColumn = policy.action.firstOrNull()?.lowercase()
    if (userEnteredCrnColumn.isNullOrBlank()) {
      throw ValidationException("action for LAO policy was null or empty")
    }
    val matchedCrnColumns = data.flatMap { it.keys }.toSet().filter { it.lowercase() == userEnteredCrnColumn }
    if (matchedCrnColumns.isEmpty()) {
      throw ValidationException("Data product had no columns matching the given 'crn' column in action ${data.flatMap { it.keys }.toSet()}")
    }
    if (matchedCrnColumns.size > 1) {
      throw ValidationException("Data product had multiple columns matching the given 'crn' column in action")
    }
    val crnColumn = matchedCrnColumns.first()
    val exclusions = laoDataRepository.getLaoExclusionsForUser(executionContext.userInfo.username)
    val crnColumnValues = data.map {
      if (clazz == SingleDashboardProductDefinition::class) {
        return@map (it[crnColumn] as MetricData).raw as String
      }
      it[crnColumn] as String
    }
    val restrictions = laoDataRepository.getLaoRestrictionsForCrns(crnColumnValues)
    val excludedCrns = exclusions.map { it.crn }.toSet()

    val policyBasedChanges = mutableListOf<PolicyBasedDataChange>()

    val filteredData = data.filterNot {
      val dataRowCrnValue = it[crnColumn]
      val shouldBeFilteredOut = excludedCrns.contains(dataRowCrnValue) ||
        (
          restrictions.any { restriction -> restriction.userId != executionContext.userInfo.username && restriction.crn == dataRowCrnValue } &&
            !restrictions.any { restriction -> restriction.userId == executionContext.userInfo.username && restriction.crn == dataRowCrnValue }
          )

      if (shouldBeFilteredOut) {
        policyBasedChanges.add(
          PolicyBasedDataChange(
            rowIdentifier = RowIdentifier(rowIndex = 1L),
            columnChanges = it.keys.map { colName -> ColumnChange(columnName = colName, newColumnValue = "") },
            policy = policy,
            policySpecificMetaData = LaoPolicyMetadata(
              identifyingColumn = crnColumn,
              identifyingColumnValue = dataRowCrnValue as String,
            ),
          ),
        )
      }

      shouldBeFilteredOut
    }

    return DataTransformation(filteredData, policyBasedChanges)
  }
}
