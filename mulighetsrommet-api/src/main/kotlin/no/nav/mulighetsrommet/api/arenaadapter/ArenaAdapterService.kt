package no.nav.mulighetsrommet.api.arenaadapter

import arrow.core.getOrElse
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingArenaService
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingAvtaleService
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.OpprettGjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.service.OpprettGjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskoder
import no.nav.mulighetsrommet.model.Tiltaksnummer
import org.slf4j.LoggerFactory
import java.util.UUID

class ArenaAdapterService(
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val arrangorService: ArrangorService,
    private val tiltakstypeService: TiltakstypeService,
    private val gjennomforingEnkeltplassService: GjennomforingEnkeltplassService,
    private val gjennomforingAvtaleService: GjennomforingAvtaleService,
    private val gjennomforingArenaService: GjennomforingArenaService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun upsertTiltaksgjennomforing(arenaGjennomforing: ArenaGjennomforingDbo): UUID? {
        val erTiltakMigrert = tiltakstypeService.getByArenaTiltakskode(arenaGjennomforing.arenaKode).any {
            it.tiltakskode != null && tiltakstypeService.erMigrert(it.tiltakskode)
        }
        if (erTiltakMigrert) {
            updateArenadata(arenaGjennomforing)
            return null
        }

        if (Tiltakskoder.isEgenRegiTiltak(arenaGjennomforing.arenaKode)) {
            return upsertEgenRegiTiltak(arenaGjennomforing)
        }

        if (Tiltakskoder.isGruppetiltak(arenaGjennomforing.arenaKode)) {
            throw IllegalArgumentException("Forventet ikke å motta nye gjennomføringer for tiltakskode=${arenaGjennomforing.arenaKode} fordi alle gruppetiltak skal være migrert")
        }

        if (!Tiltakskoder.isEnkeltplassTiltak(arenaGjennomforing.arenaKode)) {
            throw IllegalArgumentException("Forventet at gjennomføring skulle vært av typen enkeltplass, tiltakskode=${arenaGjennomforing.arenaKode}")
        }

        val arrangor = syncArrangorFromBrreg(Organisasjonsnummer(arenaGjennomforing.arrangorOrganisasjonsnummer))

        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(arenaGjennomforing.arenaKode).singleOrNull()
            ?: throw IllegalArgumentException("Fant ikke én tiltakstype for arenaKode=${arenaGjennomforing.arenaKode}")

        val sluttDato = arenaGjennomforing.sluttDato
        if (sluttDato == null || sluttDato >= ArenaMigrering.EnkeltplassSluttDatoCutoffDate) {
            val upsert = OpprettGjennomforingEnkeltplass(
                id = arenaGjennomforing.id,
                tiltakstypeId = tiltakstype.id,
                arrangorId = arrangor.id,
                navn = arenaGjennomforing.navn,
                startDato = arenaGjennomforing.startDato,
                sluttDato = arenaGjennomforing.sluttDato,
                status = mapAvslutningsstatus(arenaGjennomforing.avslutningsstatus),
                deltidsprosent = arenaGjennomforing.deltidsprosent,
                antallPlasser = arenaGjennomforing.antallPlasser,
                arenaTiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
                arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
            )
            gjennomforingEnkeltplassService.upsert(upsert)
        } else {
            val upsert = OpprettGjennomforingArena(
                id = arenaGjennomforing.id,
                tiltakstypeId = tiltakstype.id,
                arrangorId = arrangor.id,
                navn = arenaGjennomforing.navn,
                startDato = arenaGjennomforing.startDato,
                sluttDato = arenaGjennomforing.sluttDato,
                status = mapAvslutningsstatus(arenaGjennomforing.avslutningsstatus),
                deltidsprosent = arenaGjennomforing.deltidsprosent,
                antallPlasser = arenaGjennomforing.antallPlasser,
                arenaTiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
                arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
                oppstart = GjennomforingOppstartstype.ENKELTPLASS,
                pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
            )
            gjennomforingArenaService.upsert(upsert)
        }

        return null
    }

    suspend fun removeSanityTiltaksgjennomforing(sanityId: UUID) {
        sanityService.deleteSanityTiltaksgjennomforing(sanityId)
    }

    private suspend fun upsertEgenRegiTiltak(
        arenaGjennomforing: ArenaGjennomforingDbo,
    ): UUID? {
        require(Tiltakskoder.isEgenRegiTiltak(arenaGjennomforing.arenaKode)) {
            "Gjennomføring for tiltakstype ${arenaGjennomforing.arenaKode} skal ikke skrives til Sanity"
        }

        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(arenaGjennomforing.arenaKode).singleOrNull()
            ?: throw IllegalArgumentException("Fant ikke én tiltakstype for arenaKode=${arenaGjennomforing.arenaKode}")

        val sluttDato = arenaGjennomforing.sluttDato
        return if (sluttDato == null || sluttDato.isAfter(TiltaksgjennomforingSluttDatoCutoffDate)) {
            sanityService.createOrPatchSanityTiltaksgjennomforing(arenaGjennomforing, tiltakstype.sanityId)
        } else {
            null
        }
    }

    private fun updateArenadata(arenaGjennomforing: ArenaGjennomforingDbo) {
        val previous = db.session { queries.gjennomforing.getGjennomforing(arenaGjennomforing.id) }
            ?: throw IllegalStateException("Tiltakstype tiltakskode=${arenaGjennomforing.arenaKode} er migrert, men gjennomføring fra Arena er ukjent")

        val arenadata = Gjennomforing.ArenaData(
            tiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
            ansvarligNavEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
        )
        when (previous) {
            is GjennomforingAvtale,
            -> gjennomforingAvtaleService.updateArenaData(arenaGjennomforing.id, arenadata)

            is GjennomforingEnkeltplass,
            -> gjennomforingEnkeltplassService.updateArenaData(arenaGjennomforing.id, arenadata)

            is GjennomforingArena,
            -> gjennomforingArenaService.updateArenaData(arenaGjennomforing.id, arenadata)
        }
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): ArrangorDto {
        return arrangorService.getArrangorOrSyncFromBrreg(orgnr).getOrElse { error ->
            if (error == BrregError.NotFound) {
                logger.warn("Virksomhet med orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
            }
            throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
        }
    }
}

private fun mapAvslutningsstatus(avslutningsstatus: Avslutningsstatus): GjennomforingStatusType = when (avslutningsstatus) {
    Avslutningsstatus.IKKE_AVSLUTTET -> GjennomforingStatusType.GJENNOMFORES
    Avslutningsstatus.AVBRUTT -> GjennomforingStatusType.AVBRUTT
    Avslutningsstatus.AVLYST -> GjennomforingStatusType.AVLYST
    Avslutningsstatus.AVSLUTTET -> GjennomforingStatusType.AVSLUTTET
}
