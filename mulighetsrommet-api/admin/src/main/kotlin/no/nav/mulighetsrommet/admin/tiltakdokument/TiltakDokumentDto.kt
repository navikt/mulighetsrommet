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
    val sanityId: UUID? = null,
    val tiltaksnummer: String? = null,
    val tiltakstype: Tiltakstype,
    val stedForGjennomforing: String? = null,
    val arrangor: Arrangor? = null,
    val faneinnhold: Faneinnhold? = null,
    val beskrivelse: String? = null,
    val publisert: Boolean = false,
    val administratorer: List<Administrator> = emptyList(),
    val kontorstruktur: List<Kontorstruktur> = emptyList(),
    val kontaktpersoner: List<Kontaktperson> = emptyList(),
    val arrangorKontaktpersoner: List<ArrangorKontaktperson> = emptyList(),
) {
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
