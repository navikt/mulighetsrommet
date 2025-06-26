package no.nav.mulighetsrommet.api.avtale.task

import arrow.core.getOrElse
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatusDto
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.AvtaleStatus
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Prismodell
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class BackfillAvtale(
    private val db: ApiDatabase,
    private val arrangorService: ArrangorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun execute() {
        val gjennomforinger = getForhandsgodkjenteGjennomforingerUtenAvtale()

        gjennomforinger.forEach {
            generateAvtale(it)
        }
    }

    private suspend fun generateAvtale(gjennomforingId: UUID): Unit = db.transaction {
        logger.info("Genererer avtale for gjennomføring id=$gjennomforingId")

        val gjennomforing = checkNotNull(queries.gjennomforing.get(gjennomforingId))

        val orgnr = gjennomforing.arrangor.organisasjonsnummer
        logger.info("Utleder arrangør for orgnr=$orgnr")

        val arrangor = arrangorService.getArrangorOrSyncFromBrreg(orgnr).getOrElse {
            if (it is BrregError.FjernetAvJuridiskeArsaker) {
                logger.warn("Arrangør med orgnr $orgnr er fjernet fra brreg. Kan ikke opprette avtale for gjennomføring id=$gjennomforingId")
                return
            }

            throw IllegalStateException("Klarte ikke hente arrangør med orgnr: $orgnr Error: $it")
        }

        val arrangorHovedenhet = arrangor.overordnetEnhet?.let { orgnr ->
            arrangorService.getArrangorOrSyncFromBrreg(orgnr).getOrElse {
                if (it is BrregError.FjernetAvJuridiskeArsaker) {
                    logger.warn("Bedrift med orgnr $orgnr er fjernet fra brreg. Kan ikke opprette avtale for gjennomføring id=$gjennomforingId")
                    return
                }

                throw IllegalStateException("Klarte ikke hente overordnet enhet for arrangor $it Error: $it")
            }
        }

        if (arrangorHovedenhet == null) {
            logger.warn("Fant ikke overordnet enhet for $orgnr. Slettet dato=${arrangor.slettetDato}")
        }

        val avtale = AvtaleDbo(
            id = UUID.randomUUID(),
            navn = gjennomforing.navn,
            tiltakstypeId = gjennomforing.tiltakstype.id,
            avtalenummer = null,
            sakarkivNummer = null,
            arrangor = arrangorHovedenhet?.let {
                AvtaleDbo.Arrangor(
                    hovedenhet = it.id,
                    underenheter = listOf(gjennomforing.arrangor.id),
                    kontaktpersoner = listOf(),
                )
            },
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            status = AvtaleStatus.AKTIV,
            navEnheter = listOf(),
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            prisbetingelser = null,
            antallPlasser = null,
            administratorer = listOf(),
            beskrivelse = null,
            faneinnhold = null,
            personopplysninger = listOf(),
            personvernBekreftet = false,
            amoKategorisering = null,
            opsjonsmodell = Opsjonsmodell(
                type = OpsjonsmodellType.VALGFRI_SLUTTDATO,
                opsjonMaksVarighet = null,
            ),
            utdanningslop = null,
            prismodell = Prismodell.FORHANDSGODKJENT,
        )

        logger.info("Oppretter avtale for gjennomføring uten avtale avtaleId=${avtale.id}, gjennomforingId=${gjennomforing.id}")
        queries.avtale.upsert(avtale)
        when (gjennomforing.status) {
            is GjennomforingStatusDto.Gjennomfores -> {
                queries.avtale.setStatus(
                    avtale.id,
                    if (arrangorHovedenhet == null) AvtaleStatus.UTKAST else AvtaleStatus.AKTIV,
                    null,
                    null,
                )
            }

            is GjennomforingStatusDto.Avsluttet -> {
                queries.avtale.setStatus(avtale.id, AvtaleStatus.AVSLUTTET, null, null)
            }

            is GjennomforingStatusDto.Avlyst, is GjennomforingStatusDto.Avbrutt -> {
                queries.avtale.setStatus(
                    avtale.id,
                    AvtaleStatus.AVBRUTT,
                    LocalDateTime.now(),
                    AvbruttAarsak.Annet("Gjennomføring er avbrutt"),
                )
            }
        }
        queries.gjennomforing.setAvtaleId(gjennomforing.id, avtale.id)
    }

    private fun getForhandsgodkjenteGjennomforingerUtenAvtale(): List<UUID> = db.session {
        @Language("PostgreSQL")
        val query = """
            select id
            from gjennomforing_admin_dto_view
            where avtale_id is null
              and (tiltakstype_tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING' or tiltakstype_tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET')
        """.trimIndent()

        session.list(queryOf(query)) { it.uuid("id") }
    }
}
