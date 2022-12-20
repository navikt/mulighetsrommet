package no.nav.mulighetsrommet.arena.adapter.utils

import com.github.kagkarlsson.scheduler.serializer.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.charset.StandardCharsets

class DbSchedulerKotlinSerializer : Serializer {
    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(data: Any): ByteArray {
        val serializer = serializer(data.javaClass)
        return Json.encodeToString(serializer, data).toByteArray(StandardCharsets.UTF_8)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T : Any?> deserialize(clazz: Class<T>, serializedData: ByteArray): T {
        // Hackish workaround?
        // https://github.com/Kotlin/kotlinx.serialization/issues/1134
        // https://stackoverflow.com/questions/64284767/replace-jackson-with-kotlinx-serialization-in-javalin-framework/64285478#64285478

        @Suppress("UNCHECKED_CAST")
        val deserializer = serializer(clazz) as KSerializer<T>
        return Json.decodeFromString(deserializer, String(serializedData, StandardCharsets.UTF_8))
    }
}
