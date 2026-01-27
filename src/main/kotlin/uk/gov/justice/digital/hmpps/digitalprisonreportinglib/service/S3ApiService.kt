package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@Service
class S3ApiService(
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String,
  private val s3Client: S3Client,
) {
  fun doesObjectExist(tableId: String): Boolean {
    val splitLocation = s3location.split("/")
    val request = HeadObjectRequest.builder().bucket(splitLocation[0]).key("${splitLocation[1]}/$tableId").build()
    try {
      val response = s3Client.headObject(request)
      return !response.deleteMarker()
    } catch (exception: Exception) {
      when (exception) {
        is NoSuchKeyException -> {
          return false
        }
        else -> throw exception
      }
    }
  }
}
