package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class XlsxStreamingSupport {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    const val XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  }

  /**
   * Streams a report as XLSX.
   *
   * Unlike the CSV equivalent there is no gzip branch — XLSX is a zip container, so it is
   * already compressed — and no UTF-8 BOM, which is a CSV-only hint to Excel.
   */
  fun streamXlsx(
    reportId: String,
    reportVariantId: String,
    response: HttpServletResponse,
    streamFun: (ReportRowWriter) -> Unit,
  ) {
    response.contentType = XLSX_CONTENT_TYPE
    response.setHeader(
      "Content-Disposition",
      "attachment; filename=$reportId-$reportVariantId.xlsx",
    )

    log.debug("Streaming xlsx content...")
    XlsxRowWriter(response.outputStream).use { rowWriter ->
      streamFun(rowWriter)
    }
    log.debug("Successfully wrote the entire xlsx data.")
  }
}
