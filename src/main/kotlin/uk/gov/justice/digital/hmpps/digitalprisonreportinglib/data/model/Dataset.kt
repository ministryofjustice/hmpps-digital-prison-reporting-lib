package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.JsonAdapter
import kotlinx.serialization.Serializable
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QueryDeserializer

@Serializable
data class Dataset(
  val id: String,
  val name: String,
  val datasource: String,
  @Serializable(with = QueryDeserializer::class)
  @JsonAdapter(QueryDeserializer::class)
  val query: List<MultiphaseQuery>,
  val schema: Schema,
  val parameters: List<Parameter>? = null,
  val schedule: String? = null,
) : Identified {
  override fun getIdentifier() = this.id
}
