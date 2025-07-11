package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model

import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.annotations.SerializedName

enum class LoadType(@JsonValue val value: String) {
  @SerializedName("sync")
  SYNC("sync"),

  @SerializedName("async")
  ASYNC("async"),
}
