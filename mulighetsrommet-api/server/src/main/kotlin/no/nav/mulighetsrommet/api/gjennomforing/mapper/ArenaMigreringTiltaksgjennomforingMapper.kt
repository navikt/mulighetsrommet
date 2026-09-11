package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.contracts.arenamigrering.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.contracts.arenamigrering.ArenaTiltaksgjennomforingStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime

object ArenaMigreringTiltaksgjennomforingMapper {
    fun from(
        gjennomforing: Gjennomforing,
        arenaId: Int?,
    ): ArenaMigreringTiltaksgjennomforingDto? {
        val enhetsnummer = gjennomforing.arena?.ansvarligNavEnhet
            ?: (gjennomforing as? GjennomforingAvtale)?.kontorstruktur?.firstOrNull()?.region?.enhetsnummer?.value
            ?: (gjennomforing as? GjennomforingEnkeltplass)?.ansvarligEnhet?.enhetsnummer?.value
            ?: error("navRegion or arenaAnsvarligEnhet was null! Should not be possible!")

        val arenaStatus = when (gjennomforing) {
            is GjennomforingAvtale -> toArenaStatus(gjennomforing.status)
            is GjennomforingArena -> toArenaStatus(gjennomforing.status)
            is GjennomforingEnkeltplass -> toArenaStatus(gjennomforing.status)
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

    private fun toArenaStatus(status: GjennomforingAvtaleStatus): ArenaTiltaksgjennomforingStatus = when (status) {
        is GjennomforingAvtaleStatus.Gjennomfores -> ArenaTiltaksgjennomforingStatus.GJENNOMFORES
        is GjennomforingAvtaleStatus.Avsluttet -> ArenaTiltaksgjennomforingStatus.AVSLUTTET
        is GjennomforingAvtaleStatus.Avbrutt -> ArenaTiltaksgjennomforingStatus.AVBRUTT
        is GjennomforingAvtaleStatus.Avlyst -> ArenaTiltaksgjennomforingStatus.AVLYST
    }

    private fun toArenaStatus(status: GjennomforingEnkeltplassStatus): ArenaTiltaksgjennomforingStatus = when (status) {
        is GjennomforingEnkeltplassStatus.UtkastTilPamelding,
        is GjennomforingEnkeltplassStatus.SoktInn,
        is GjennomforingEnkeltplassStatus.VenterPaOppstart,
        is GjennomforingEnkeltplassStatus.Deltar,
        -> ArenaTiltaksgjennomforingStatus.GJENNOMFORES

        is GjennomforingEnkeltplassStatus.Fullfort -> ArenaTiltaksgjennomforingStatus.AVSLUTTET

        is GjennomforingEnkeltplassStatus.IkkeAktuell,
        is GjennomforingEnkeltplassStatus.Avbrutt,
        is GjennomforingEnkeltplassStatus.AvbruttUtkast,
        is GjennomforingEnkeltplassStatus.Feilregistrert,
        -> ArenaTiltaksgjennomforingStatus.AVBRUTT
    }
}
