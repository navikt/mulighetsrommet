package no.nav.mulighetsrommet.api.domain.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Avtale(
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
    val status: AvtaleStatus,
    val administratorer: List<Administrator>,
    val navEnheter: Set<NavEnhetNummer>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val opplaring: OpplaringKategorisering?,
    val opsjonsmodell: Opsjonsmodell,
    val opsjonerRegistrert: List<OpsjonLogg>,
    val prismodeller: List<Prismodell>,
) {

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

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
    )

    @Serializable
    data class OpsjonLogg(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        @Serializable(with = LocalDateTimeSerializer::class)
        val createdAt: LocalDateTime,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val forrigeSluttDato: LocalDate,
        val status: OpsjonLoggStatus,
    )
}
