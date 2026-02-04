package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client

@Service
@ConditionalOnBean(S3Client::class)
class S3ApiService(
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String,
  private val s3Client: S3Client,
) {
  fun doesPrefixExist(tableId: String): Boolean {
    val splitLocation = s3location.split("/")
    val bucket = splitLocation[0]
    val prefix = "${splitLocation[1]}/$tableId/"

    val response = s3Client.listObjectsV2 {
      it.bucket(bucket)
        .prefix(prefix)
        .maxKeys(1)
    }

    return response.contents().isNotEmpty()
  }
}
