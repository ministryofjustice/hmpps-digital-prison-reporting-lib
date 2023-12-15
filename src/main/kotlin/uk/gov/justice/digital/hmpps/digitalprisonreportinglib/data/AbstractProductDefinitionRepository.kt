package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

abstract class AbstractProductDefinitionRepository : ProductDefinitionRepository {

  override fun getSingleReportProductDefinition(definitionId: String, reportId: String): SingleReportProductDefinition {
    val schemaRefPrefix = "\$ref:"
    val productDefinition = getProductDefinition(definitionId)
    val reportDefinition = productDefinition.report
      .filter { it.id == reportId }
      .ifEmpty { throw ValidationException("Invalid report variant id provided: $reportId") }
      .first()

    val dataSetId = reportDefinition.dataset.removePrefix(schemaRefPrefix)
    val dataSet = productDefinition.dataset
      .filter { it.id == dataSetId }
      .ifEmpty { throw ValidationException("Invalid dataSetId in report: $dataSetId") }
      .first()

    return SingleReportProductDefinition(
      id = definitionId,
      name = productDefinition.name,
      description = productDefinition.description,
      metadata = productDefinition.metadata,
      datasource = productDefinition.datasource.first(),
      dataset = dataSet,
      report = reportDefinition,
      policy = productDefinition.policy,
    )
  }
}
