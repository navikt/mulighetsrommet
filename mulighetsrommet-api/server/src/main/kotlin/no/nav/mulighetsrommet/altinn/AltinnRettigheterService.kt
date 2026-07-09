package no.nav.mulighetsrommet.altinn

import arrow.core.Either
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.right
import no.nav.mulighetsrommet.altinn.db.BedriftRettigheterDbo
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.Duration
import java.time.Instant

class AltinnRettigheterService(
    private val config: Config = Config(),
    private val db: ApiDatabase,
    private val altinnClient: AltinnClient,
) {
    data class Config(
        val rettighetExpiryDuration: Duration = Duration.ofHours(1),
    )

    suspend fun getRettigheter(norskIdent: NorskIdent): Either<AltinnError, List<BedriftRettigheter>> = db.session {
        val bedriftRettigheter = queries.altinnRettigheter.getRettigheter(norskIdent)
        return if (bedriftRettigheter.isEmpty() || anyExpiredBefore(bedriftRettigheter, Instant.now())) {
            syncRettigheter(norskIdent)
        } else {
            bedriftRettigheter.map { it.toBedriftRettigheter() }.right()
        }
    }

    private fun anyExpiredBefore(bedriftRettigheter: List<BedriftRettigheterDbo>, now: Instant): Boolean {
        return bedriftRettigheter.any { (_, rettigheter) ->
            rettigheter.any { rettighet -> rettighet.expiry.isBefore(now) }
        }
    }

    private suspend fun QueryContext.syncRettigheter(
        norskIdent: NorskIdent,
    ): Either<AltinnError, List<BedriftRettigheter>> = either {
        val rettigheter = altinnClient.hentRettigheter(norskIdent).bind()

        queries.altinnRettigheter.upsertRettigheter(
            norskIdent = norskIdent,
            bedriftRettigheter = rettigheter,
            expiry = Instant.now().plus(config.rettighetExpiryDuration),
        )

        rettigheter
    }
}

fun BedriftRettigheterDbo.toBedriftRettigheter() = BedriftRettigheter(
    organisasjonsnummer = organisasjonsnummer,
    rettigheter = rettigheter.map { it.rettighet },
)
