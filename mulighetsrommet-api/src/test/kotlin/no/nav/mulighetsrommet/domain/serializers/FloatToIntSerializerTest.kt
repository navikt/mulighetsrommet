package no.nav.mulighetsrommet.domain.serializers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.serializers.FloatToIntSerializer

class FloatToIntSerializerTest : FunSpec({
    test("should decode floats to nearest int") {
        Json.decodeFromString(FloatToIntSerializer, "12.3") shouldBe 12
        Json.decodeFromString(FloatToIntSerializer, "12.4") shouldBe 12
        Json.decodeFromString(FloatToIntSerializer, "12.5") shouldBe 13
    }

    test("should encode int as int") {
        Json.encodeToString(FloatToIntSerializer, 12) shouldBe "\"12\""
    }
})
