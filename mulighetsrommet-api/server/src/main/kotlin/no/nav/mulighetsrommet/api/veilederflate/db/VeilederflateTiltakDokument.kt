package no.nav.mulighetsrommet.api.veilederflate.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

data class VeilederflateTiltakDokument(
    val id: UUID,
    val sanityId: UUID?,
    val navn: String,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val tiltaksnummer: String?,
    val tiltakskode: Tiltakskode,
    val stedForGjennomforing: String?,
    val navEnheter: List<NavEnhet>,
    val kontaktpersoner: List<Kontaktperson>,
    val arrangor: Arrangor?,
    val arrangorKontaktpersoner: List<ArrangorKontaktperson>,
) {
    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val organisasjonsnummer: String,
    )

    @Serializable
    data class Kontaktperson(
        val navIdent: NavIdent,
        val navn: String,
        val epost: String?,
        val mobilnummer: String?,
        val hovedenhet: NavEnhetNummer,
        val beskrivelse: String?,
    )

    @Serializable
    data class ArrangorKontaktperson(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val telefon: String?,
        val epost: String?,
        val beskrivelse: String?,
    )

    @Serializable
    data class NavEnhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
        val type: NavEnhetType,
        val overordnetEnhet: NavEnhetNummer?,
    )
}
