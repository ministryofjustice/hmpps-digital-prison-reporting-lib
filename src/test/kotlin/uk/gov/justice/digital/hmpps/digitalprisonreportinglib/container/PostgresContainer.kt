package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {
  private val log = LoggerFactory.getLogger(this::class.java)
  fun startPostgresqlIfNotRunning(): PostgreSQLContainer<Nothing>? {
    if (isPostgresRunning()) {
      return null
    }

    val logConsumer = Slf4jLogConsumer(log).withPrefix("postgresql")

    return PostgreSQLContainer<Nothing>("postgres:16").apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      withDatabaseName("datamart")
      withExposedPorts(5432)
      withUsername("test")
      withPassword("test")
      setWaitStrategy(Wait.forListeningPort())
      withReuse(false)
      withCreateContainerCmdModifier {
        it.withHostConfig(HostConfig().withPortBindings(PortBinding(Ports.Binding.bindPort(5432), ExposedPort(5432))))
      }
      start()
      followOutput(logConsumer)
    }
  }

  private fun isPostgresRunning(): Boolean = try {
    val serverSocket = ServerSocket(5432)
    serverSocket.localPort == 0
  } catch (e: IOException) {
    true
  }
}