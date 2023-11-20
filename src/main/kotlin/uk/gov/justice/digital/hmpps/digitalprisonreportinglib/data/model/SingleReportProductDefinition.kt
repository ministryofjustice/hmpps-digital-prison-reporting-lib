package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class SingleReportProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metadata: MetaData,
  val dataSource: DataSource,
  val dataset: Dataset,
  val report: Report,
)
