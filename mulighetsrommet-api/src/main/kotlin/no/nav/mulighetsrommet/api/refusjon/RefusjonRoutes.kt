package no.nav.mulighetsrommet.api.refusjon

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravDto
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.refusjonRoutes() {
    val db: Database by inject()
    val tilsagnRepository: TilsagnRepository by inject()

    route("/refusjonskrav/{id}") {
        get {
            val id = call.parameters.getOrFail<UUID>("id")

            val krav = db.session {
                val krav = Queries.refusjonskrav.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                val kostnadsteder = tilsagnRepository
                    .getTilsagnTilRefusjon(krav.gjennomforing.id, krav.beregning.input.periode)
                    .map { it.kostnadssted }
                RefusjonKravKompakt.fromRefusjonskravDto(krav, kostnadsteder)
            }

            call.respond(krav)
        }
    }

    route("/tiltaksgjennomforinger/{id}/refusjonskrav") {
        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = db.session {
                    Queries.refusjonskrav.getByGjennomforing(id).map { krav ->
                        val kostnadsteder = tilsagnRepository
                            .getTilsagnTilRefusjon(krav.gjennomforing.id, krav.beregning.input.periode)
                            .map { it.kostnadssted }
                        RefusjonKravKompakt.fromRefusjonskravDto(krav, kostnadsteder)
                    }
                }

                call.respond(krav)
            }
        }
    }
}

@Serializable
data class RefusjonKravKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: RefusjonskravStatus,
    val beregning: Beregning,
    val kostnadsteder: List<NavEnhetDbo>,
) {
    @Serializable
    data class Beregning(
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val belop: Int,
    )

    companion object {
        fun fromRefusjonskravDto(krav: RefusjonskravDto, kostnadsteder: List<NavEnhetDbo>) = RefusjonKravKompakt(
            id = krav.id,
            status = krav.status,
            beregning = krav.beregning.let {
                Beregning(
                    periodeStart = it.input.periode.start,
                    periodeSlutt = it.input.periode.getLastDate(),
                    belop = it.output.belop,
                )
            },
            kostnadsteder = kostnadsteder,
        )
    }
}
