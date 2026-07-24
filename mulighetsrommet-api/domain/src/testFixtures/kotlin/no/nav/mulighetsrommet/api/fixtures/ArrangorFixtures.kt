package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

object ArrangorFixtures {

    fun kontaktperson(
        id: UUID = UUID.randomUUID(),
        navn: String = "Kari Nordmann",
        arrangorId: UUID,
    ) = ArrangorKontaktperson(
        id = id,
        arrangorId = arrangorId,
        navn = navn,
        beskrivelse = null,
        telefon = null,
        epost = "kari@example.com",
        ansvarligFor = listOf(ArrangorKontaktperson.Ansvar.AVTALE),
    )

    val hovedenhet = Arrangor.Norsk.opprett(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = "AS",
        navn = "Hovedenhet AS",
    )

    val underenhet1 = Arrangor.Norsk.opprett(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("976663934"),
        organisasjonsform = "BEDR",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        navn = "Underenhet 1 AS",
    )

    val underenhet2 = Arrangor.Norsk.opprett(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("890765789"),
        organisasjonsform = "BEDR",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        navn = "Underenhet 2 AS",
    )

    object Utenlandsk {
        val hovedenhet = Arrangor.Utenlandsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("100000001"),
            navn = "Utenlandsk Tiger",
        )
    }
}
