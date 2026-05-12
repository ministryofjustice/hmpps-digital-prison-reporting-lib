package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration

interface Scheduler {
  val scheduler: ThreadPoolTaskScheduler
  val duration: Duration
  fun scheduledFunction()
  fun schedule() {
    try {
      scheduler.scheduleAtFixedRate(::scheduledFunction, duration)
    } finally {
      // Cleanup
    }
  }
}
