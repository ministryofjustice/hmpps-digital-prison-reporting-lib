package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class SingleReportProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metaData: MetaData,
  val dataSource: DataSource,
  val dataSet: DataSet,
  val report: Report,
)
