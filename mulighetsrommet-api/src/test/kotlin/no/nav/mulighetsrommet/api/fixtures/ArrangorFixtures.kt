package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.*

object ArrangorFixtures {
    object Fretex {
        val hovedenhet = ArrangorDto(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("983982433"),
            organisasjonsform = "AS",
            navn = "FRETEX AS",
        )

        val underenhet1 = ArrangorDto(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("992943084"),
            organisasjonsform = "BEDR",
            overordnetEnhet = Organisasjonsnummer("983982433"),
            navn = "FRETEX AS AVD OSLO",
        )
    }

    val hovedenhet = ArrangorDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = "AS",
        navn = "Hovedenhet AS",
    )

    val underenhet1 = ArrangorDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("976663934"),
        organisasjonsform = "BEDR",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        navn = "Underenhet 1 AS",
    )

    val underenhet2 = ArrangorDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("890765789"),
        organisasjonsform = "BEDR",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        navn = "Underenhet 2 AS",
    )
}
