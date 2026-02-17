package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.zip.GZIPOutputStream

@Component
class CsvStreamingSupport {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun streamCsv(
    reportId: String,
    reportVariantId: String,
    request: HttpServletRequest,
    response: HttpServletResponse,
    streamFun: (Writer) -> Unit,
  ) {
    response.contentType = "text/csv"

    val acceptsGzip =
      request.getHeader("Accept-Encoding")?.contains("gzip") == true

    val outputStream =
      if (acceptsGzip) {
        log.debug("Streaming gzip content...")
        response.setHeader("Content-Encoding", "gzip")
        GZIPOutputStream(response.outputStream)
      } else {
        log.debug("Streaming csv content...")
        response.outputStream
      }

    response.setHeader(
      "Content-Disposition",
      "attachment; filename=$reportId-$reportVariantId.csv",
    )

    outputStream.use { out ->
      OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
        // Writes 0xEF 0xBB 0xBF to the start of the file so that it's recognised as UTF-8 with BOM so that Excel opens it properly.
        writer.write("\ufeff")
        streamFun(writer)
        log.debug(
          "Successfully wrote the entire ${if (acceptsGzip) "gzip" else "csv"} data.",
        )
      }
    }
  }
}
