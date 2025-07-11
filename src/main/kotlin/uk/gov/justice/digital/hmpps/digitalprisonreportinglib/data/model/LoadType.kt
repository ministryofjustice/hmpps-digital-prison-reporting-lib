package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class LoadType {
  @SerializedName("sync")
  SYNC,

  @SerializedName("async")
  ASYNC,
}
