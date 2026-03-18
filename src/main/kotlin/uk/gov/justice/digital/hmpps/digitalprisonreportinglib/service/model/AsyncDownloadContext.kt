package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model

data class AsyncDownloadContext(
  private val core: CoreDownloadContext,
) : DownloadContext by core
