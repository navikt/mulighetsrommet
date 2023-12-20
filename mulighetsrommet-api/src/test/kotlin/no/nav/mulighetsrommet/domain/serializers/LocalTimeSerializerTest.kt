package no.nav.mulighetsrommet.domain.serializers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import java.time.LocalTime

class LocalTimeSerializerTest : FunSpec({
    test("should decode correctly") {
        Json.decodeFromString(LocalTimeSerializer, "\"09:00\"") shouldBe LocalTime.of(9, 0)
        Json.decodeFromString(LocalTimeSerializer, "\"09:01\"") shouldBe LocalTime.of(9, 1)
        Json.decodeFromString(LocalTimeSerializer, "\"12:32\"") shouldBe LocalTime.of(12, 32)
    }

    test("should encode correctly") {
        Json.encodeToString(LocalTimeSerializer, LocalTime.of(12, 0, 0)) shouldBe "\"12:00:00\""
        Json.encodeToString(LocalTimeSerializer, LocalTime.of(0, 12)) shouldBe "\"00:12:00\""
        Json.encodeToString(LocalTimeSerializer, LocalTime.of(12, 32)) shouldBe "\"12:32:00\""
    }
})
