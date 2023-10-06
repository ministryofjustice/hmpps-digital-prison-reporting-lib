package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class ProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metaData: MetaData,
  val dataSource: List<DataSource> = emptyList(),
  val dataSet: List<DataSet> = emptyList(),
  val report: List<Report> = emptyList(),
)
