package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import java.time.Duration

interface Scheduler {
  val scheduler: ThreadPoolTaskScheduler
  val duration: Duration
  fun scheduledFunction()
  fun schedule() {
    try {
      scheduler.scheduleAtFixedRate(::scheduledFunction, duration)
    } finally {
      ExecutionContext.clear() // Clear this because it's using ThreadLocal, we don't want threads in a thread pool to reuse an existing prepopulated context
    }
  }
}
