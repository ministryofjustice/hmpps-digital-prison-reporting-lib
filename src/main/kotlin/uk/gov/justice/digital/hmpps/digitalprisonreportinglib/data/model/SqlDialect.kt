package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

enum class SqlDialect {

  @SerialName("oracle/11g")
  @SerializedName("oracle/11g")
  ORACLE11g,

  @SerialName("postgres/19")
  @SerializedName("postgres/19")
  POSTGRES19,

  @SerialName("redshift/4")
  @SerializedName("redshift/4")
  REDSHIFT4,

  @SerialName("athena/3")
  @SerializedName("athena/3")
  ATHENA3,
}
