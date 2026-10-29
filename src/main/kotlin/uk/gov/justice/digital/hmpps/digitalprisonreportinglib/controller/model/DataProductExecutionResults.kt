package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType

data class RowIdentifier(
  val rowIndex: String,
)

data class ColumnChange(
  val columnName: String,
  val newColumnValue: String,
)

sealed class PolicySpecificMetadata(
  val policyType: PolicyType,
)

data class LaoPolicyMetadata(
  val identifyingColumn: String,
  val identifyingColumnValue: String,
) : PolicySpecificMetadata(PolicyType.LAO)

data class PolicyBasedDataChange(
  val rowIdentifier: RowIdentifier,
  val columnChanges: List<ColumnChange>,
  val policy: Policy,
  val policySpecificMetaData: PolicySpecificMetadata,
)

enum class ResultType {
  REPORT,
  DASHBOARD,
}

abstract class DataProductExecutionResults(
  val resultType: ResultType,
) {
  abstract val policyBasedDataChanges: List<PolicyBasedDataChange>
}

data class ReportExecutionResults(
  override val policyBasedDataChanges: List<PolicyBasedDataChange> = emptyList(),
  val data: List<Map<String, Any?>>,
) : DataProductExecutionResults(ResultType.REPORT)

data class DashboardExecutionResults(
  override val policyBasedDataChanges: List<PolicyBasedDataChange> = emptyList(),
  val data: List<Map<String, Any?>>,
) : DataProductExecutionResults(ResultType.DASHBOARD)
