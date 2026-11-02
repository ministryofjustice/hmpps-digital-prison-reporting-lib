package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ParameterType {
  @SerialName("boolean")
  @SerializedName("boolean")
  Boolean,

  @SerialName("date")
  @SerializedName("date")
  Date,

  @SerialName("datetime")
  @SerializedName("datetime")
  DateTime,

  @SerialName("double")
  @SerializedName("double")
  Double,

  @SerialName("float")
  @SerializedName("float")
  Float,

  @SerialName("int")
  @SerializedName("int")
  Integer,

  @SerialName("long")
  @SerializedName("long")
  Long,

  @SerialName("string")
  @SerializedName("string")
  String,

  @SerialName("time")
  @SerializedName("time")
  Time,

  @SerialName("timestamp")
  @SerializedName("timestamp")
  Timestamp,
}
