package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.kafka.dto.TiltakAvtaleHendelseDto
import no.nav.tiltak.historikk.model.ArbeidsgiverAvtale
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class ReplikerTiltakAvtaleKafkaConsumer(
    private val db: TiltakshistorikkDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer tiltak-avtale-hendelse med avtaleId=$key")

        val hendelse = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltakAvtaleHendelseDto?>(message)

        when {
            hendelse == null -> {
                logger.info("Mottok tombstone for avtale med id=$key, sletter avtalen")
                queries.arbeidsgiverAvtale.delete(key)
            }

            !Organisasjonsnummer.isValid(hendelse.bedriftNr) -> {
                logger.warn("Mottok hendelse med ugydlig organisasjonsnummer: ${hendelse.bedriftNr}")
                return
            }

            else -> {
                val incoming = hendelse.toArbeidsgiverAvtale()
                val stored = queries.arbeidsgiverAvtale.get(key)

                if (stored != null && incoming.oppdatertTidspunkt < stored.oppdatertTidspunkt) {
                    logger.info("Hopper over avtale med id=$key fordi innkommende oppdatertTidspunkt er eldre enn lagret")
                    return@session
                }

                query { queries.arbeidsgiverAvtale.upsert(incoming) }.onLeft {
                    logger.warn("Feil under konsumering av avtale med id=$key", it.error)
                    throw it.error
                }
            }
        }
    }
}

fun TiltakAvtaleHendelseDto.toArbeidsgiverAvtale() = ArbeidsgiverAvtale(
    avtaleId = avtaleId,
    norskIdent = deltakerFnr,
    organisasjonsnummer = Organisasjonsnummer(bedriftNr),
    tiltakstype = tiltakstype.toArbeidsgiverAvtaleTiltakstype(),
    startDato = startDato,
    sluttDato = sluttDato,
    status = avtaleStatus.toArbeidsgiverAvtaleStatus(),
    stillingsprosent = stillingprosent,
    dagerPerUke = antallDagerPerUke,
    opprettetTidspunkt = opprettetTidspunkt.atZone(ZoneId.of("Europe/Oslo")).toInstant().truncatedTo(ChronoUnit.MICROS),
    oppdatertTidspunkt = sistEndret.truncatedTo(ChronoUnit.MICROS),
)

private fun TiltakAvtaleHendelseDto.Tiltakstype.toArbeidsgiverAvtaleTiltakstype() = when (this) {
    TiltakAvtaleHendelseDto.Tiltakstype.ARBEIDSTRENING -> Tiltakskode.ARBEIDSTRENING
    TiltakAvtaleHendelseDto.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD
    TiltakAvtaleHendelseDto.Tiltakstype.VARIG_LONNSTILSKUDD -> Tiltakskode.VARIG_LONNSTILSKUDD
    TiltakAvtaleHendelseDto.Tiltakstype.MENTOR -> Tiltakskode.MENTOR
    TiltakAvtaleHendelseDto.Tiltakstype.INKLUDERINGSTILSKUDD -> Tiltakskode.INKLUDERINGSTILSKUDD
    TiltakAvtaleHendelseDto.Tiltakstype.SOMMERJOBB -> Tiltakskode.SOMMERJOBB
    TiltakAvtaleHendelseDto.Tiltakstype.VTAO -> Tiltakskode.VTAO
    TiltakAvtaleHendelseDto.Tiltakstype.FIREARIG_LONNSTILSKUDD -> Tiltakskode.FIREARIG_LONNSTILSKUDD
}

private fun TiltakAvtaleHendelseDto.Status.toArbeidsgiverAvtaleStatus(): ArbeidsgiverAvtale.Status = when (this) {
    TiltakAvtaleHendelseDto.Status.PAABEGYNT -> ArbeidsgiverAvtale.Status.PAABEGYNT
    TiltakAvtaleHendelseDto.Status.MANGLER_GODKJENNING -> ArbeidsgiverAvtale.Status.MANGLER_GODKJENNING
    TiltakAvtaleHendelseDto.Status.KLAR_FOR_OPPSTART -> ArbeidsgiverAvtale.Status.KLAR_FOR_OPPSTART
    TiltakAvtaleHendelseDto.Status.GJENNOMFORES -> ArbeidsgiverAvtale.Status.GJENNOMFORES
    TiltakAvtaleHendelseDto.Status.AVSLUTTET -> ArbeidsgiverAvtale.Status.AVSLUTTET
    TiltakAvtaleHendelseDto.Status.AVBRUTT -> ArbeidsgiverAvtale.Status.AVBRUTT
    TiltakAvtaleHendelseDto.Status.ANNULLERT -> ArbeidsgiverAvtale.Status.ANNULLERT
}
