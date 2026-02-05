package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.JsonAdapter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QueryDeserializer

data class Dataset(
  val id: String,
  val name: String,
  val datasource: String,
  @JsonAdapter(QueryDeserializer::class)
  val query: List<MultiphaseQuery>,
  val schema: Schema,
  val parameters: List<Parameter>? = null,
  val schedule: String? = null,
  val multiphaseQuery: List<MultiphaseQuery>? = null,
) : Identified {
  override fun getIdentifier() = this.id
}
