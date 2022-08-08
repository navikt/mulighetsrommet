package no.nav.mulighetsrommet.arena.adapter.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

object JobRunners {
    private val logger = LoggerFactory.getLogger(JobRunners.javaClass)

    fun executeBackgroundJob(job: Job = Job(), run: suspend CoroutineScope.() -> Unit): Job {
        return CoroutineScope(job).launch {
            logger.info("Running background job ${job.key}")
            try {
                val time = measureTimeMillis {
                    run()
                }
                logger.info("Background job ${job.key} finished in ${time}ms")
            } catch (e: Throwable) {
                logger.warn("Background job was cancelled with exception: ${e.stackTraceToString()}")
                throw e
            }
        }
    }
}
