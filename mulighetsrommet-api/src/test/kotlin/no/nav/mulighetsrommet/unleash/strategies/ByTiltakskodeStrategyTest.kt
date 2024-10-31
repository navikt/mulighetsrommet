package no.nav.mulighetsrommet.unleash.strategies

import io.getunleash.UnleashContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ByTiltakskodeStrategyTest : FunSpec({
    val strategy = ByTiltakskodeStrategy()

    test("Skal returnere false når ingen UnleashContext er sendt inn") {
        strategy.isEnabled(mutableMapOf()) shouldBe false
    }

    test("Skal returnere false når ingen tiltakskoder er konfigurert") {
        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to ""),
            createContext(emptyMap()),
        ) shouldBe false
    }

    test("Skal returnere false når tiltakskoder ikke spørres om") {
        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING"),
            createContext(emptyMap()),
        ) shouldBe false
    }

    test("Skal returnere false når ikke alle tiltakskoder ikke matcher") {
        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING"),
            createContext(mapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "AVKLARING")),
        ) shouldBe false

        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING"),
            createContext(mapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "AVKLARING,OPPFOLGING")),
        ) shouldBe false

        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING"),
            createContext(mapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING,OPPFOLGING")),
        ) shouldBe false
    }

    test("Skal returnere true når alle tiltakskoder matcher") {
        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING"),
            createContext(mapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING")),
        ) shouldBe true

        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING"),
            createContext(mapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "AVKLARING")),
        ) shouldBe true

        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING"),
            createContext(mapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING")),
        ) shouldBe true

        strategy.isEnabled(
            mutableMapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING,OPPFOLGING"),
            createContext(mapOf(ByTiltakskodeStrategy.TILTAKSKODER_PARAM to "ARBEIDSFORBEREDENDE_TRENING,AVKLARING")),
        ) shouldBe true
    }
})

private fun createContext(properties: Map<String, String>) = UnleashContext("", "", "", properties)
