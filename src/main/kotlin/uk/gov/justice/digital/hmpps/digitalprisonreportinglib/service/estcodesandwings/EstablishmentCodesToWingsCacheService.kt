package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings

import com.google.common.cache.Cache
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentToWing
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentsToWingsRepository

@Service
class EstablishmentCodesToWingsCacheService(
  private val establishmentsToWingsRepository: EstablishmentsToWingsRepository?,
  private val establishmentCodesCache: Cache<String, List<EstablishmentToWing>>,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  fun getEstablishmentsAndPopulateCacheIfNeeded(): Map<String, List<EstablishmentToWing>> {
    if (establishmentCodesCache.size() == 0L) {
      log.debug("Establishments cache was empty.")
      refresh()
    }
    return establishmentCodesCache.asMap()
  }

  fun refresh() {
    log.debug("Initiating refresh of establishments cache...")
    val stopWatch = StopWatch.createStarted()
    if (establishmentCodesCache.size() != 0L) {
      establishmentCodesCache.invalidateAll()
    }
    val establishmentToWingsMap = establishmentsToWingsRepository?.executeStatementWaitAndGetResult() ?: emptyMap()
    if (establishmentToWingsMap.isNotEmpty()) {
      establishmentCodesCache.putAll(establishmentToWingsMap)
      stopWatch.stop()
      log.info("Establishments and wings cache refreshed in ${stopWatch.time}.")
    }
  }
}
