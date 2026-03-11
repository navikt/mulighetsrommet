package no.nav.tiltak.okonomi.helved

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

/**
 * Statusmelding fra Hel ved
 *
 * Forenkler integrasjon mot Oppdragssystemet (OS) beregner og overfører utbetalingsoppdrag til Utbetaling Reskonto (UR)
 */
@Serializable
data class HelVedStatus(
    val status: Status,
    val detaljer: StatusDetaljer?,
    val error: StatusError?,
) {
    @Serializable
    enum class Status {
        /** Hel ved har lest meldingen */
        MOTTATT,

        /** Valideringsfeil hos Hel ved eller noe feiler mot OS */
        FEILET,

        /**
         * OS har motatt meldingen. Kan bli liggende med denne statusen utenfor åpningstiden.
         * Vanlig åpningstid hos OS/UR er 06:00 til 21:00 hver dag med unntak av helger og røde dager.
         * */
        HOS_OPPDRAG,

        /** Kvittert ut fra OS */
        OK,
    }

    @Serializable
    data class StatusDetaljer(
        val ytelse: String,
        val linjer: List<DetaljerLinje>,
    ) {
        @Serializable
        data class DetaljerLinje(
            val behandlingId: String,
            @Serializable(with = LocalDateSerializer::class)
            val fom: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val tom: LocalDate,
            /** >= 1 */
            val vedtakssats: Int,
            /** >= 1 */
            val belop: Int,
            val klassekode: String,
        )
    }

    @Serializable
    data class StatusError(
        val statusCode: Int,
        val msg: String,
        val doc: String,
    )
}
