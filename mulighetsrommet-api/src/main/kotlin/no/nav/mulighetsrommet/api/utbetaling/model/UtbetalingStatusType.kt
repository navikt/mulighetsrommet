package no.nav.mulighetsrommet.api.utbetaling.model

enum class UtbetalingStatusType {
    /**
     * Systemet har generert utbetalingen, men den er enda ikke godkjent av arrangør.
     */
    GENERERT,

    /**
     * Arrangør eller Nav-ansatt har opprettet utbetalingen.
     */
    INNSENDT,

    /**
     * Saksbehandler hos Nav har utført kostnadsfordeling og sendt utbetalingen til attestering.
     */
    TIL_ATTESTERING,

    /**
     * Attestant har sendt utbetalingen i retur.
     */
    RETURNERT,

    /**
     * Attestant har godkjent (attestert) utbetalingen.
     */
    FERDIG_BEHANDLET,
}
