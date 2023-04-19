package no.nav.mulighetsrommet.arena.adapter.models.db

import no.nav.mulighetsrommet.arena.adapter.models.arena.Administrasjonskode
import no.nav.mulighetsrommet.arena.adapter.models.arena.Handlingsplan
import no.nav.mulighetsrommet.arena.adapter.models.arena.Rammeavtale
import java.time.LocalDateTime
import java.util.*

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val tiltaksgruppekode: String,
    val tiltakskode: String,
    val registrertIArenaDato: LocalDateTime,
    val sistEndretIArenaDato: LocalDateTime,
    val fraDato: LocalDateTime,
    val tilDato: LocalDateTime,
    val rettPaaTiltakspenger: Boolean,
    val administrasjonskode: Administrasjonskode,
    val sendTilsagnsbrevTilDeltaker: Boolean,
    val tiltakstypeSkalHaAnskaffelsesprosess: Boolean,
    val maksAntallPlasser: Int? = null,
    val maksAntallSokere: Int? = null,
    val harFastAntallPlasser: Boolean? = null,
    val skalSjekkeAntallDeltakere: Boolean? = null,
    val visLonnstilskuddskalkulator: Boolean,
    val rammeavtale: Rammeavtale? = null,
    val opplaeringsgruppe: String? = null,
    val handlingsplan: Handlingsplan? = null,
    val tiltaksgjennomforingKreverSluttdato: Boolean,
    val maksPeriodeIMnd: Int? = null,
    val tiltaksgjennomforingKreverMeldeplikt: Boolean? = null,
    val tiltaksgjennomforingKreverVedtak: Boolean,
    val tiltaksgjennomforingReservertForIABedrift: Boolean,
    val harRettPaaTilleggsstonader: Boolean,
    val harRettPaaUtdanning: Boolean,
    val tiltaksgjennomforingGenererTilsagnsbrevAutomatisk: Boolean,
    val visBegrunnelseForInnsoking: Boolean,
    val sendHenvisningsbrevOgHovedbrevTilArbeidsgiver: Boolean,
    val sendKopibrevOgHovedbrevTilArbeidsgiver: Boolean,
)
