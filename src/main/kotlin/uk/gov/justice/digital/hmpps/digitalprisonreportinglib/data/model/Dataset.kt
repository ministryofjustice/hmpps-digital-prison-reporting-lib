package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Dataset(
  val id: String,
  val name: String,
  val datasource: String,
  val query: String,
  val schema: Schema,
  val parameters: List<Parameter>? = null,
  val schedule: String? = null,
  val multiphaseQuery: List<MultiphaseQuery>? = null,
) : Identified() {
  override fun getIdentifier() = this.id
}
