package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

enum class DatasourceConnection {
  @SerializedName("federated")
  FEDERATED,

  @SerialName("datawarehouse")
  @SerializedName("datawarehouse")
  DATA_WAREHOUSE,

  @SerialName("awsdatacatalog")
  @SerializedName("awsdatacatalog")
  AWS_DATA_CATALOG,
}
