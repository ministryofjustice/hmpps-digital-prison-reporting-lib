package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class SqlDialect {

  @SerializedName("oracle/11g")
  ORACLE11g,

  @SerializedName("postgres/19")
  POSTGRES19,

  @SerializedName("redshift/4")
  REDSHIFT4,

  @SerializedName("athena/3")
  ATHENA3,
}
