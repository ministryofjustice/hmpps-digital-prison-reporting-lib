package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Feature(
  type: FeatureType
)

enum class FeatureType {
  print,
}