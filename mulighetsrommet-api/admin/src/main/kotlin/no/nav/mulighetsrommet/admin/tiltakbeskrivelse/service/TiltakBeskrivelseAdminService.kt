package no.nav.mulighetsrommet.admin.tiltakbeskrivelse.service

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseDto
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseHandling
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class TiltakBeskrivelseRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltaksnummer: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val stedForGjennomforing: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID? = null,
    val arrangorKontaktpersoner: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        > = emptySet(),
    val administratorer: Set<NavIdent> = emptySet(),
    val veilederinformasjon: VeilederinfoRequest = VeilederinfoRequest(),
) {
    @Serializable
    data class VeilederinfoRequest(
        val faneinnhold: Faneinnhold? = null,
        val beskrivelse: String? = null,
        val navRegioner: Set<NavEnhetNummer> = emptySet(),
        val navKontorer: Set<NavEnhetNummer> = emptySet(),
        val navAndreEnheter: Set<NavEnhetNummer> = emptySet(),
        val kontaktpersoner: Set<Kontaktperson> = emptySet(),
    )

    @Serializable
    data class Kontaktperson(
        val navIdent: NavIdent,
        val beskrivelse: String? = null,
    )
}

class TiltakBeskrivelseAdminService(
    private val db: AdminDatabase,
    private val navAnsattService: NavAnsattService,
) {
    suspend fun upsert(request: TiltakBeskrivelseRequest, navIdent: NavIdent): Either<List<FieldError>, TiltakBeskrivelseDto> {
        val tiltakstype = db.session { repository.tiltakstype.get(request.tiltakstypeId) }
            ?: return FieldError.of("Fant ikke tiltakstype", TiltakBeskrivelseRequest::tiltakstypeId).nel().left()

        val previous = db.session { queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(request.id) }

        return TiltakBeskrivelseValidator.validate(request, tiltakstype, previous)
            .onRight {
                it.kontaktpersoner.forEach { navAnsattService.addUserToKontaktpersoner(it.navIdent) }
            }
            .map { tiltakBeskrivelse ->
                db.transaction {
                    val isNew = previous == null
                    repository.tiltakBeskrivelse.save(tiltakBeskrivelse)
                    val dto = queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(request.id)!!
                    val operation = if (isNew) "Opprettet tiltaksbeskrivelse" else "Endret tiltaksbeskrivelse"
                    logEndring(operation, dto, navIdent)
                    dto
                }
            }
    }

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: NavIdent): Unit = db.transaction {
        queries.tiltakBeskrivelse.setPublisert(id, publisert)
        val dto = queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(id)!!
        val operation = if (publisert) "Publiserte tiltaksbeskrivelse" else "Avpubliserte tiltaksbeskrivelse"
        logEndring(operation, dto, navIdent)
    }

    fun getHandlinger(ansatt: NavAnsatt): Set<TiltakBeskrivelseHandling> {
        return TiltakBeskrivelseHandling.entries
            .filter { tilgangTilHandling(ansatt, it) }
            .toSet()
    }

    internal fun QueryContext.logEndring(tekst: String, tiltakBeskrivelse: TiltakBeskrivelseDto, endretAv: NavIdent) {
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.TILTAK_BESKRIVELSE,
            tekst,
            endretAv,
            tiltakBeskrivelse.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(tiltakBeskrivelse) }
    }

    companion object {
        private fun tilgangTilHandling(ansatt: NavAnsatt, handling: TiltakBeskrivelseHandling): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            return when (handling) {
                TiltakBeskrivelseHandling.PUBLISER -> skrivGjennomforing
                TiltakBeskrivelseHandling.REDIGER -> skrivGjennomforing
                TiltakBeskrivelseHandling.FORHANDSVIS_I_MODIA -> true
            }
        }
    }
}
