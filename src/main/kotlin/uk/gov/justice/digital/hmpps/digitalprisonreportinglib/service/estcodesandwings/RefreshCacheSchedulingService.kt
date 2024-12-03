package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshCacheSchedulingService(
  val refreshCacheTaskScheduler: ThreadPoolTaskScheduler,
  val establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
) {
  @PostConstruct
  fun scheduleCacheRefresh() {
    refreshCacheTaskScheduler
      .scheduleAtFixedRate(
        { establishmentCodesToWingsCacheService.refresh() },
        Duration.ofDays(1),
      )
  }

  @PreDestroy
  fun cleanUp() {
    refreshCacheTaskScheduler.initiateShutdown()
    refreshCacheTaskScheduler.shutdown()
  }
}
