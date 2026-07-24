package no.nav.mulighetsrommet.admin.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class AvtaleDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val avtalenummer: String?,
    val sakarkivNummer: SakarkivNummer?,
    val arrangor: ArrangorHovedenhet?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val avtaletype: Avtaletype,
    val status: Status,
    val administratorer: List<Avtale.Administrator>,
    val kontorstruktur: List<Kontorstruktur>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val opplaring: OpplaringKategoriseringDetaljer?,
    val opsjonsmodell: Opsjonsmodell,
    val opsjonerRegistrert: List<Avtale.OpsjonLogg>,
    val prismodeller: List<PrismodellDto>,
) {
    @Serializable
    data class Status(
        val type: AvtaleStatusType,
        val status: DataElement.Status,
    )

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class ArrangorHovedenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
        val underenheter: List<ArrangorUnderenhet>,
        val kontaktpersoner: List<ArrangorKontaktperson>,
    )

    @Serializable
    data class ArrangorUnderenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class ArrangorKontaktperson(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val beskrivelse: String?,
        val telefon: String?,
        val epost: String,
    )
}
