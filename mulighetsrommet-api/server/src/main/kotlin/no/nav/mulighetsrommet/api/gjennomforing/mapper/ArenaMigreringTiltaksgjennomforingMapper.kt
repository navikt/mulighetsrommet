package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.contracts.arenamigrering.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.contracts.arenamigrering.ArenaTiltaksgjennomforingStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.model.GjennomforingStatusType

object ArenaMigreringTiltaksgjennomforingMapper {
    fun from(
        gjennomforing: Gjennomforing,
        arenaId: Int?,
    ): ArenaMigreringTiltaksgjennomforingDto? {
        val enhetsnummer = gjennomforing.arena?.ansvarligNavEnhet
            ?: (gjennomforing as? GjennomforingAvtale)?.kontorstruktur?.firstOrNull()?.region?.enhetsnummer?.value
            ?: (gjennomforing as? GjennomforingEnkeltplass)?.ansvarligEnhet?.enhetsnummer?.value
            ?: error("navRegion or arenaAnsvarligEnhet was null! Should not be possible!")

        val arenaStatus = when (gjennomforing.status) {
            GjennomforingStatusType.GJENNOMFORES -> ArenaTiltaksgjennomforingStatus.GJENNOMFORES
            GjennomforingStatusType.AVSLUTTET -> ArenaTiltaksgjennomforingStatus.AVSLUTTET
            GjennomforingStatusType.AVBRUTT -> ArenaTiltaksgjennomforingStatus.AVBRUTT
            GjennomforingStatusType.AVLYST -> ArenaTiltaksgjennomforingStatus.AVLYST
        }

        val startDato = gjennomforing.startDato ?: return null

        return ArenaMigreringTiltaksgjennomforingDto(
            id = gjennomforing.id,
            tiltakskode = checkNotNull(gjennomforing.tiltakstype.tiltakskode.arenakode) {
                "${gjennomforing.tiltakstype.tiltakskode} har ingen mapping til Arena"
            },
            startDato = startDato,
            sluttDato = gjennomforing.sluttDato,
            opprettetTidspunkt = gjennomforing.opprettetTidspunkt.tilNorskLocalDateTime(),
            endretTidspunkt = gjennomforing.oppdatertTidspunkt.tilNorskLocalDateTime(),
            navn = gjennomforing.navn,
            orgnummer = gjennomforing.arrangor.organisasjonsnummer.value,
            antallPlasser = gjennomforing.antallPlasser,
            status = arenaStatus,
            arenaId = arenaId,
            enhet = enhetsnummer,
            apentForInnsok = when (gjennomforing) {
                is GjennomforingAvtale -> gjennomforing.apentForPamelding
                is GjennomforingEnkeltplass, is GjennomforingArena -> false
            },
            deltidsprosent = gjennomforing.deltidsprosent,
        )
    }
}
