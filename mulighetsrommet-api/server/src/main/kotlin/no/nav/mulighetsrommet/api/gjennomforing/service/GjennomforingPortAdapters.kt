package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.admin.deltaker.DeltakelserError
import no.nav.mulighetsrommet.admin.deltaker.DeltakelserPort
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingAuditLog
import no.nav.mulighetsrommet.admin.personalia.PersonaliaPort
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerError
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserRequest
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.auditlog.AuditLog.auditLogger
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

class AmtDeltakelserPort(
    private val amtDeltakerClient: AmtDeltakerClient,
    private val personaliaService: PersonaliaService,
) : DeltakelserPort {
    override suspend fun finnDeltakerIds(
        norskIdent: NorskIdent,
        accessType: AccessType.OBO.AzureAd,
    ): Either<DeltakelserError, List<UUID>> {
        personaliaService.navAnsattTilgangTilPerson(
            norskIdent,
            PersonaliaService.OnBehalfOf.NavAnsatt(accessType),
        )?.let {
            return DeltakelserError.ManglerTilgang(it).left()
        }

        val deltakelser = amtDeltakerClient.hentDeltakelser(DeltakelserRequest(norskIdent), accessType).getOrElse {
            if (it == AmtDeltakerError.NotFound) {
                return emptyList<UUID>().right()
            }

            return DeltakelserError.OppslagFeilet.left()
        }

        return (deltakelser.aktive + deltakelser.historikk).map { it.deltakerId }.distinct().right()
    }
}

class PersonaliaServicePort(
    private val personaliaService: PersonaliaService,
) : PersonaliaPort {
    override suspend fun getPersonalia(deltakerId: UUID, accessType: AccessType.OBO.AzureAd): Personalia {
        return personaliaService.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(accessType))
    }

    override suspend fun getPersonaliaAsSystem(deltakerId: UUID): Personalia {
        return personaliaService.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System)
    }

    override suspend fun navAnsattTilgangTilPerson(
        norskIdent: NorskIdent,
        accessType: AccessType.OBO.AzureAd,
    ): AvvistGrunn? {
        return personaliaService.navAnsattTilgangTilPerson(
            norskIdent,
            PersonaliaService.OnBehalfOf.NavAnsatt(accessType),
        )
    }
}

class CefGjennomforingAuditLog : GjennomforingAuditLog {
    override fun visEnkeltplassDeltaker(gjennomforingId: UUID, navIdent: NavIdent, norskIdent: NorskIdent) {
        val message = CefMessage.builder()
            .applicationName("Tiltaksadministrasjon")
            .loggerName("mulighetsrommet-api")
            .event(CefMessageEvent.ACCESS)
            .name("Tiltaksadministrasjon - Se enkeltplassdeltaker")
            .severity(CefMessageSeverity.INFO)
            .sourceUserId(navIdent.value)
            .destinationUserId(norskIdent.value)
            .timeEnded(System.currentTimeMillis())
            .extension(
                "msg",
                "Nav-ansatt har hentet informasjon om tiltaksdeltaker på tiltaksgjennomføring med id=$gjennomforingId'.",
            )
            .build()
        auditLogger.log(message)
    }
}
