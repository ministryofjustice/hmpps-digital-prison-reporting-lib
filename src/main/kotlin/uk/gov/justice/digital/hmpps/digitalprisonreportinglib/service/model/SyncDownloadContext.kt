package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter

data class SyncDownloadContext(
  private val core: CoreDownloadContext,
  val reportFilter: ReportFilter? = null,
  val query: String,
  val policyEngineResult: String,
) : DownloadContext by core
