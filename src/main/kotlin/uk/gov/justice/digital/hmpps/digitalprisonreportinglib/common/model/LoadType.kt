package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model

import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LoadType(@JsonValue val value: String) {
  @SerialName("sync")
  @SerializedName("sync")
  SYNC("sync"),

  @SerialName("async")
  @SerializedName("async")
  ASYNC("async"),
}
