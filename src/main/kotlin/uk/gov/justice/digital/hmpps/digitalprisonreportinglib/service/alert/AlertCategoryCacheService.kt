package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.alert

import com.google.common.cache.Cache
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategory
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategoryRepository

@Service
class AlertCategoryCacheService(
  private val alertCategoryRepository: AlertCategoryRepository?,
  private val alertCodesCache: Cache<String, List<AlertCategory>>,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val ALERT_CACHE_KEY = "ALERT_CODES"
  }

  fun getAlertCodesCacheIfNeeded(): Map<String, List<AlertCategory>> {
    if (alertCodesCache.size() == 0L) {
      log.debug("AlertCategory cache was empty.")
      refresh()
    }
    return alertCodesCache.asMap()
  }

  fun refresh() {
    log.debug("Initiating refresh of alert category cache...")
    val stopWatch = StopWatch.createStarted()
    if (alertCodesCache.size() != 0L) {
      alertCodesCache.invalidateAll()
    }
    val alertCategories = alertCategoryRepository?.executeStatementWaitAndGetResult() ?: emptyList()
    if (alertCategories.isNotEmpty()) {
      alertCodesCache.put(ALERT_CACHE_KEY, alertCategories)
      stopWatch.stop()
      log.info("Alert Category cache refreshed in ${stopWatch.time}.")
    }
  }
}
