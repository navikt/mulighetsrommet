package no.nav.mulighetsrommet.tasks

import com.github.kagkarlsson.scheduler.serializer.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.charset.StandardCharsets

/**
 * Serializer to be used in combination with instances of `db-scheduler` tasks that require specific task data to run.
 *
 * See the documentation for more info:
 * - https://github.com/kagkarlsson/db-scheduler/tree/431f162e6a2e43774a0b6a283aa576bd9c2cf5bf#serializers
 */
class DbSchedulerKotlinSerializer : Serializer {
    override fun serialize(data: Any): ByteArray {
        val serializer = serializer(data.javaClass)
        return Json.encodeToString(serializer, data).toByteArray(StandardCharsets.UTF_8)
    }

    override fun <T : Any?> deserialize(clazz: Class<T>, serializedData: ByteArray): T {
        // Hackish workaround?
        // https://github.com/Kotlin/kotlinx.serialization/issues/1134
        // https://stackoverflow.com/questions/64284767/replace-jackson-with-kotlinx-serialization-in-javalin-framework/64285478#64285478

        @Suppress("UNCHECKED_CAST")
        val deserializer = serializer(clazz) as KSerializer<T>
        return Json.decodeFromString(deserializer, String(serializedData, StandardCharsets.UTF_8))
    }
}
