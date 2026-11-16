package no.nav.mulighetsrommet.maam

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import java.util.UUID

@DslMarker
annotation class MaamDsl

@MaamDsl
class MaamConfig {
    var database: (() -> Database)? = null
    var environment: NaisEnv = NaisEnv.current()

    internal var kafkaConfig: MaamKafkaConfig? = null
    internal var tasksConfig: MaamTasksConfig? = null

    fun kafka(
        orchestrator: () -> KafkaConsumerOrchestrator,
        configure: MaamKafkaConfig.() -> Unit,
    ) {
        check(kafkaConfig == null) { "Maam Kafka is already configured" }
        kafkaConfig = MaamKafkaConfig(orchestrator).apply(configure)
    }

    fun tasks(configure: MaamTasksConfig.() -> Unit) {
        tasksConfig = (tasksConfig ?: MaamTasksConfig()).apply(configure)
    }
}

@MaamDsl
class MaamKafkaConfig internal constructor(
    internal val orchestrator: () -> KafkaConsumerOrchestrator,
) {
    internal var consumersEnabled = false
    internal var failedRecordsEnabled = false

    fun consumers() {
        consumersEnabled = true
    }

    fun failedRecords() {
        failedRecordsEnabled = true
    }
}

@MaamDsl
class MaamTasksConfig {
    internal var scheduledEnabled = false
    internal val handlers = linkedMapOf<String, suspend ApplicationCall.() -> TaskResult>()

    fun scheduled() {
        scheduledEnabled = true
    }

    fun task(name: String, execute: suspend () -> TaskResult) {
        register(name) { execute() }
    }

    inline fun <reified Input : Any> task(
        name: String,
        noinline execute: suspend (Input) -> TaskResult,
    ) {
        register(name) { execute(receive<Input>()) }
    }

    @PublishedApi
    internal fun register(name: String, execute: suspend ApplicationCall.() -> TaskResult) {
        require(name.isNotBlank()) { "Maam task name must not be blank" }
        require(name !in handlers) { "Maam task '$name' is already registered" }
        handlers[name] = execute
    }
}

sealed interface TaskResult {
    data class Scheduled(val id: UUID) : TaskResult
    data class Completed(val message: String) : TaskResult
    data class Rejected(val message: String) : TaskResult
    data class Forbidden(val message: String) : TaskResult
}
