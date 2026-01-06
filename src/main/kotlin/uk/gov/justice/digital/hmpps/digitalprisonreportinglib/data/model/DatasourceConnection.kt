package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class DatasourceConnection {
  @SerializedName("federated")
  FEDERATED,

  @SerializedName("datawarehouse")
  DATA_WAREHOUSE,

  @SerializedName("awsdatacatalog")
  AWS_DATA_CATALOG,
}
