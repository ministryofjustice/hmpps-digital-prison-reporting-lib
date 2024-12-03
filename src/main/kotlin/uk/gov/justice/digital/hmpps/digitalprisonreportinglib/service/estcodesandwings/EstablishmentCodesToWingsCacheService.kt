package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings

import com.google.common.cache.Cache
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentAndWing
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentsAndWingsRepository

@Service
class EstablishmentCodesToWingsCacheService(
  private val establishmentsAndWingsRepository: EstablishmentsAndWingsRepository,
  private val establishmentCodesCache: Cache<String, List<EstablishmentAndWing>>,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  fun getEstablishmentsAndPopulateCacheIfNeeded(): Map<String, List<EstablishmentAndWing>> {
    if (establishmentCodesCache.size() == 0L) {
      refresh()
    }
    return establishmentCodesCache.asMap()
  }

  fun refresh() {
    val stopWatch = StopWatch.createStarted()
    if (establishmentCodesCache.size() != 0L) {
      establishmentCodesCache.invalidateAll()
    }
    val establishmentToWingsMap = establishmentsAndWingsRepository.executeStatementWaitAndGetResult()
    if (establishmentToWingsMap.isNotEmpty()) {
      establishmentCodesCache.putAll(establishmentToWingsMap)
      stopWatch.stop()
      log.info("Establishments and wings cache refreshed in ${stopWatch.duration.seconds} sec.")
    }

  }
}
