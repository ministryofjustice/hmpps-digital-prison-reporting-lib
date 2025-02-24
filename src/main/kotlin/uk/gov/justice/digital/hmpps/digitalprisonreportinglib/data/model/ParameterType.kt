package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class ParameterType {
  @SerializedName("boolean")
  Boolean,

  @SerializedName("date")
  Date,

  @SerializedName("datetime")
  DateTime,

  @SerializedName("double")
  Double,

  @SerializedName("float")
  Float,

  @SerializedName("int")
  Integer,

  @SerializedName("long")
  Long,

  @SerializedName("string")
  String,

  @SerializedName("time")
  Time,

  @SerializedName("timestamp")
  Timestamp,
}
