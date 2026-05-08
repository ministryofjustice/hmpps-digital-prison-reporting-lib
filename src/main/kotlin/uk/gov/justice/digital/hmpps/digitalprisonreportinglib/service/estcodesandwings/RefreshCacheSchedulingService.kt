package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.Scheduler
import java.time.Duration

@Service
class RefreshCacheSchedulingService(
  @Qualifier("refreshCacheTaskScheduler") override val scheduler: ThreadPoolTaskScheduler,
  @Value("\${dpr.lib.establishmentsAndWings.cache.durationMinutes:#{1440L}}")
  private val establishmentsCacheDurationMinutes: Long = 1440L,
  val establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
) : Scheduler {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override val duration: Duration = Duration.ofMinutes(establishmentsCacheDurationMinutes)
  override fun scheduledFunction() {
    log.debug("Scheduling establishments cache refresh every $establishmentsCacheDurationMinutes min.")
    establishmentCodesToWingsCacheService.refresh()
  }

  @PostConstruct
  override fun schedule() {
    super.schedule()
  }

  @PreDestroy
  fun cleanUp() {
    scheduler.initiateShutdown()
    scheduler.shutdown()
  }
}
