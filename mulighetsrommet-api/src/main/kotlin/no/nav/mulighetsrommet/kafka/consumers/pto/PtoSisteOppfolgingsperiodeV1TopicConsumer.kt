package no.nav.mulighetsrommet.kafka.consumers.pto

import arrow.core.getOrElse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.domain.serializers.ZonedDateTimeSerializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*

class PtoSisteOppfolgingsperiodeV1TopicConsumer(
    config: Config,
    private val tiltakshistorikk: TiltakshistorikkRepository,
    private val pdlClient: PdlClient,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement) {
        val sisteOppfolgingsperiode = JsonIgnoreUnknownKeys.decodeFromJsonElement<SisteOppfolgingsperiodeV1?>(message)
        when (sisteOppfolgingsperiode) {
            null -> {}
            else -> {
                if (sisteOppfolgingsperiode.aktorId.isEmpty() || sisteOppfolgingsperiode.startDato == null) {
                    log.error("Ugyldig data for siste oppfolging periode på bruker")
                    return
                }
                if (sisteOppfolgingsperiode.sluttDato != null &&
                    sisteOppfolgingsperiode.startDato.isAfter(sisteOppfolgingsperiode.sluttDato)
                ) {
                    log.error("Ugyldig start/slutt dato for siste oppfolging periode på bruker")
                    return
                }

                if (sisteOppfolgingsperiode.sluttDato == null) {
                    return // Oppfolging er ikke avsluttet - Noop
                }

                val request = GraphqlRequest.HentHistoriskeIdenter(
                    ident = PdlIdent(sisteOppfolgingsperiode.aktorId),
                    grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT),
                )
                pdlClient.hentHistoriskeIdenter(request, AccessType.M2M)
                    .map { list -> list.map { NorskIdent(it.ident.value) } }
                    .getOrElse {
                        when (it) {
                            PdlError.Error -> throw Exception("Error mot pdl i konsumering av siste oppfolgingsperiode")
                            PdlError.NotFound -> {
                                log.warn("Fant ikke folkeregister ident for bruker")
                                listOf()
                            }
                        }
                    }
                    .takeIf { it.isNotEmpty() }
                    ?.let { identer ->
                        log.debug("Oppfølging avsluttet, sletter brukers tiltakshistorikk")
                        tiltakshistorikk.deleteTiltakshistorikkForIdenter(identer)
                    }
            }
        }
    }
}

@Serializable
data class SisteOppfolgingsperiodeV1(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val aktorId: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val startDato: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val sluttDato: ZonedDateTime? = null,
)
