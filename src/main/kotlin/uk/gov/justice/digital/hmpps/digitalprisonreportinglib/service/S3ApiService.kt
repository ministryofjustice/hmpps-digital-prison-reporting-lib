package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sts.StsClient

@Service
class S3ApiService(
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String,
) {

  var s3Client: S3Client = S3Client.builder().region(Region.EU_WEST_2).build()
  var stsClient: StsClient = StsClient.builder().region(Region.EU_WEST_2).build()

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun doesObjectExist(tableId: String): Boolean {
    val splitLocation = s3location.split("/")
    val request = HeadObjectRequest.builder().bucket(splitLocation[0]).key("${splitLocation[1]}/$tableId").build()
    try {
      log.info("s3 head request: {}", request)
      log.info("sts client identity: {}", stsClient.callerIdentity.arn())
      val response = s3Client.headObject(request)
      log.info("s3 response: {}", response)
      return !response.deleteMarker()
    } catch (exception: Exception) {
      log.error("s3 error", exception.stackTrace)
      when (exception) {
        is NoSuchKeyException -> {
          return false
        }
        else -> throw exception
      }
    }
  }
}
