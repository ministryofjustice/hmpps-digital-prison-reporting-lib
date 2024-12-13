package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshCacheSchedulingService(
  val refreshCacheTaskScheduler: ThreadPoolTaskScheduler,
  val establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
  @Value("\${dpr.lib.establishmentsAndWings.cache:#{1440}}")
  private val establishmentsCacheDurationMinutes: Long = 1440,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @PostConstruct
  fun scheduleCacheRefresh() {
    log.debug("Scheduling establishments cache refresh every $establishmentsCacheDurationMinutes min.")
    refreshCacheTaskScheduler
      .scheduleAtFixedRate(
        { establishmentCodesToWingsCacheService.refresh() },
        Duration.ofMinutes(establishmentsCacheDurationMinutes),
      )
  }

  @PreDestroy
  fun cleanUp() {
    refreshCacheTaskScheduler.initiateShutdown()
    refreshCacheTaskScheduler.shutdown()
  }
}
