package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.LoadType

data class VariantDefinitionSummary(
  val id: String,
  val name: String,
  val description: String? = null,
  val isMissing: Boolean = false,
  val loadType: LoadType? = null,
)
