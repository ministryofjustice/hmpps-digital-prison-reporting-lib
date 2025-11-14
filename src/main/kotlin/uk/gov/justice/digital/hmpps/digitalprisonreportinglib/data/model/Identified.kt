package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

interface Identified {
  companion object {
    const val REF_PREFIX = "\$ref:"
  }

  fun getIdentifier(): String
}
