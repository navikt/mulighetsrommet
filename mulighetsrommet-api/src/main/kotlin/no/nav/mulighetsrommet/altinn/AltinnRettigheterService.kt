package no.nav.mulighetsrommet.altinn

import no.nav.mulighetsrommet.altinn.db.BedriftRettigheterDbo
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.Duration
import java.time.LocalDateTime

class AltinnRettigheterService(
    private val config: Config = Config(Duration.ofHours(1)),
    private val db: ApiDatabase,
    private val altinnClient: AltinnClient,
) {
    data class Config(
        val rettighetExpiryDuration: Duration,
    )

    suspend fun getRettigheter(norskIdent: NorskIdent): List<BedriftRettigheter> = db.session {
        val bedriftRettigheter = queries.altinnRettigheter.getRettigheter(norskIdent)
        return if (bedriftRettigheter.isEmpty() || anyExpiredBefore(bedriftRettigheter, LocalDateTime.now())) {
            syncRettigheter(norskIdent)
        } else {
            bedriftRettigheter.map { it.toBedriftRettigheter() }
        }
    }

    private fun anyExpiredBefore(bedriftRettigheter: List<BedriftRettigheterDbo>, now: LocalDateTime): Boolean {
        return bedriftRettigheter.any { (_, rettigheter) ->
            rettigheter.any { rettighet -> rettighet.expiry.isBefore(now) }
        }
    }

    private suspend fun QueryContext.syncRettigheter(norskIdent: NorskIdent): List<BedriftRettigheter> {
        val rettigheter = altinnClient.hentRettigheter(norskIdent)

        queries.altinnRettigheter.upsertRettigheter(
            norskIdent = norskIdent,
            bedriftRettigheter = rettigheter,
            expiry = LocalDateTime.now().plusSeconds(config.rettighetExpiryDuration.seconds),
        )

        return rettigheter
    }
}

fun BedriftRettigheterDbo.toBedriftRettigheter() = BedriftRettigheter(
    organisasjonsnummer = organisasjonsnummer,
    rettigheter = rettigheter.map { it.rettighet },
)
