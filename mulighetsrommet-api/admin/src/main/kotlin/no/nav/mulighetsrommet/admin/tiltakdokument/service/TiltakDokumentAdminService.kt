package no.nav.mulighetsrommet.admin.tiltakdokument.service

import arrow.core.Either
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentDto
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentHandling
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TiltakDokumentRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val stedForGjennomforing: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID? = null,
    val arrangorKontaktpersoner: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        > = emptySet(),
    val faneinnhold: Faneinnhold? = null,
    val beskrivelse: String? = null,
    val administratorer: Set<NavIdent> = emptySet(),
    val navRegioner: Set<NavEnhetNummer> = emptySet(),
    val navKontorer: Set<NavEnhetNummer> = emptySet(),
    val navAndreEnheter: Set<NavEnhetNummer> = emptySet(),
    val kontaktpersoner: Set<Kontaktperson> = emptySet(),
) {
    @Serializable
    data class Kontaktperson(
        val navIdent: NavIdent,
        val beskrivelse: String? = null,
    )
}

class TiltakDokumentAdminService(
    private val db: AdminDatabase,
) {
    fun upsert(request: TiltakDokumentRequest): Either<List<FieldError>, TiltakDokumentDto> {
        return TiltakDokumentValidator.validate(request)
            .map { tiltakDokument ->
                db.transaction {
                    repository.tiltakDokument.save(tiltakDokument)
                    queries.tiltakDokument.getTiltakDokumentDto(request.id)!!
                }
            }
    }

    fun setPublisert(id: UUID, publisert: Boolean): Unit = db.transaction {
        queries.tiltakDokument.setPublisert(id, publisert)
    }

    fun getHandlinger(ansatt: NavAnsatt): Set<TiltakDokumentHandling> {
        return TiltakDokumentHandling.entries
            .filter { tilgangTilHandling(ansatt, it) }
            .toSet()
    }

    companion object {
        private fun tilgangTilHandling(ansatt: NavAnsatt, handling: TiltakDokumentHandling): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            return when (handling) {
                TiltakDokumentHandling.PUBLISER -> skrivGjennomforing
                TiltakDokumentHandling.REDIGER -> skrivGjennomforing
                TiltakDokumentHandling.FORHANDSVIS_I_MODIA -> true
            }
        }
    }
}
