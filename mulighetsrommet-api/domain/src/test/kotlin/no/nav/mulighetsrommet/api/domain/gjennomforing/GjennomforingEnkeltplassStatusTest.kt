package no.nav.mulighetsrommet.api.domain.gjennomforing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.GjennomforingStatusType

class GjennomforingEnkeltplassStatusTest : FunSpec({
    test("sperrer ny økonomi kun for ikke aktuell og feilregistrert") {
        GjennomforingStatusType.entries
            .filter { it.name.startsWith("ENKELTPLASS_") }
            .forEach { type ->
                GjennomforingEnkeltplassStatus.from(type).sperrerOkonomi() shouldBe
                    (
                        type == GjennomforingStatusType.ENKELTPLASS_IKKE_AKTUELL ||
                            type == GjennomforingStatusType.ENKELTPLASS_FEILREGISTRERT
                        )
            }
    }
})
