package no.nav.tiltak.okonomi.kafka

import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OkonomiFagsystem
import no.nav.tiltak.okonomi.service.TiltaksokonomiService
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class BestillingMeldingHandler(
    private val okonomi: TiltaksokonomiService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handle(
        fagsystem: OkonomiFagsystem,
        bestillingsnummer: Bestillingsnummer,
        melding: OkonomiBestillingMelding,
    ) {
        MDC.put("bestillingsnummer", bestillingsnummer.value)

        try {
            logger.info("Behandler melding for bestilling=$bestillingsnummer")

            val result = when (melding) {
                is OkonomiBestillingMelding.Bestilling -> {
                    logger.info("Oppretter bestilling=$bestillingsnummer")
                    okonomi.opprettBestilling(fagsystem, melding.payload)
                }

                is OkonomiBestillingMelding.Annullering -> {
                    logger.info("Annullerer bestilling=$bestillingsnummer")
                    okonomi.annullerBestilling(melding.payload)
                }

                is OkonomiBestillingMelding.Faktura -> {
                    logger.info("Oppretter faktura for bestilling=$bestillingsnummer")
                    okonomi.opprettFaktura(melding.payload)
                }

                is OkonomiBestillingMelding.GjorOppBestilling -> {
                    logger.info("Gjør opp bestilling=$bestillingsnummer")
                    okonomi.gjorOppBestilling(melding.payload)
                }
            }

            result.onRight {
                logger.info("Melding for bestilling=$bestillingsnummer behandlet")
            }.onLeft {
                logger.error("Feil ved behandling av melding for bestilling=$bestillingsnummer", it)
                throw it
            }
        } finally {
            MDC.clear()
        }
    }
}
