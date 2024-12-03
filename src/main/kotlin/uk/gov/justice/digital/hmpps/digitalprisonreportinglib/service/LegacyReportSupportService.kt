package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository

@Service
class LegacyReportSupportService(
  val athenaApiRepository: AthenaApiRepository,
)
