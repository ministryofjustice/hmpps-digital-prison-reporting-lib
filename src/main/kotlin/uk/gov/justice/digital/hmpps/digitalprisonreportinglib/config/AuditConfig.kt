package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.Duration.Companion.milliseconds

@Configuration
class AuditConfig {
  private val job: Job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.IO + job + CoroutineName("audit-attempt"))

  @Bean
  fun scope(): CoroutineScope = scope

  @PreDestroy
  fun drain() {
    runBlocking {
      withTimeoutOrNull(10_000.milliseconds) {
        job.children.toList().joinAll()
      }
    }
    job.cancel()
  }
}
