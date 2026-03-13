package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingTilskuddDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val lopenummer: Int,
    val periode: Periode,
    val status: Status,
    val tiltakskode: Tiltakskode,
    val tilskudd: Tilskudd,
    val belop: Int,
    val valuta: Valuta,
    val fakturaStatus: FakturaStatus?,
    val fakturaFeilmelding: String?,
    val behandletAv: NavIdent,
    val behandletTidspunkt: LocalDateTime,
    val besluttetAv: NavIdent?,
    val besluttetTidspunkt: LocalDateTime?,
) {
    enum class Status {
        OPPRETTET,
        SENDT,
        UTBETALT,
    }

    enum class FakturaStatus {
        /** Sendt til utbetalingsystem */
        SENDT,

        /** Noe gikk galt med utbetalingen */
        FEILET,

        /** Eksternt system behanlder utbetalingen */
        UNDER_BEHANDLING_EKSTERNT,

        /** Beløp vil bli sendt til bruker */
        OK,
    }

    enum class Tilskudd {
        SKOLEPENGER,
        STUDIEREISE,
        EKSAMENSAVGIFT,
        SEMESTERAVGIFT,
        INTEGRERT_BOTILBUD,
    }
}
