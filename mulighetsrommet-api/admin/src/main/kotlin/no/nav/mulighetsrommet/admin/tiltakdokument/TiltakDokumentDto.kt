package no.nav.mulighetsrommet.admin.tiltakdokument

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TiltakDokumentDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val tiltaksnummer: String?,
    val tiltakstype: Tiltakstype,
    val stedForGjennomforing: String?,
    val arrangor: Arrangor?,
    val administratorer: List<Administrator>,
    val arrangorKontaktpersoner: List<ArrangorKontaktperson>,
    val veilederinfo: Veilederinfo,
) {
    @Serializable
    data class Veilederinfo(
        val publisert: Boolean,
        val beskrivelse: String?,
        val faneinnhold: Faneinnhold?,
        val kontorstruktur: List<Kontorstruktur>,
        val kontaktpersoner: List<Kontaktperson>,
    )

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val organisasjonsnummer: String,
    )

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
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
}
