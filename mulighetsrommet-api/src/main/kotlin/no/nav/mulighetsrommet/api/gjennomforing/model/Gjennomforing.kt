package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Gjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val tiltaksnummer: String?,
    val lopenummer: String,
    val arrangor: ArrangorUnderenhet,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: ArenaNavEnhet?,
    val status: GjennomforingStatus,
    val apentForPamelding: Boolean,
    val antallPlasser: Int,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val administratorer: List<Administrator>,
    val kontorstruktur: List<Kontorstruktur>,
    val oppstart: GjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    val kontaktpersoner: List<GjennomforingKontaktperson>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val oppdatertTidspunkt: LocalDateTime,
    val publisert: Boolean,
    val deltidsprosent: Double,
    val estimertVentetid: EstimertVentetid?,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDto?,
    val stengt: List<StengtPeriode>,
) {

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
    )

    @Serializable
    data class ArrangorUnderenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val kontaktpersoner: List<ArrangorKontaktperson>,
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
    data class EstimertVentetid(
        val verdi: Int,
        val enhet: String,
    )

    @Serializable
    data class StengtPeriode(
        val id: Int,
        @Serializable(with = LocalDateSerializer::class)
        val start: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val slutt: LocalDate,
        val beskrivelse: String,
    )
}
