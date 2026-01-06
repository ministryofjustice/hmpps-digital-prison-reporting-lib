package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.LoadType

data class DashboardDefinitionSummary(
  val id: String,
  val name: String,
  val description: String? = null,
  val loadType: LoadType? = null,
)
