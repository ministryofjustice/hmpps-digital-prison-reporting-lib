package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class ProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metadata: MetaData,
  val dataSource: List<DataSource> = emptyList(),
  val dataset: List<Dataset> = emptyList(),
  val report: List<Report> = emptyList(),
  val policy: List<Policy> = emptyList(),
)
