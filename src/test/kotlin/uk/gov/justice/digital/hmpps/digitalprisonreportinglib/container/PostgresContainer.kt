package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container

import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

private data class ConnectionInfo(
  val jdbcUrl: String,
  val username: String,
  val password: String,
)

object PostgresContainer {
  private val log = LoggerFactory.getLogger(this::class.java)
  private val instance: ConnectionInfo by lazy { startPostgresqlIfNotRunning() }
  val jdbcUrl: String get() = instance.jdbcUrl
  val username: String get() = instance.username
  val password: String get() = instance.password
  private fun startPostgresqlIfNotRunning(): ConnectionInfo {
    if (isPostgresRunning()) {
      return ConnectionInfo("jdbc:postgresql://localhost:5432/datamart", "test", "test")
    }

    val logConsumer = Slf4jLogConsumer(log).withPrefix("postgresql")

    val container = PostgreSQLContainer<Nothing>("postgres:16").apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      // Tests startup faster than connections can be let go - without this, you get too many clients exception
      withCommand("postgres", "-c", "max_connections=1000")
      withDatabaseName("datamart")
      withUsername("test")
      withPassword("test")
      setWaitStrategy(Wait.forListeningPort())
      withReuse(false)
      start()
      followOutput(logConsumer)
    }

    return ConnectionInfo(container.jdbcUrl, container.username, container.password)
  }

  private fun isPostgresRunning(): Boolean = try {
    ServerSocket(5432).use {}
    false
  } catch (_: IOException) {
    true
  }
}
