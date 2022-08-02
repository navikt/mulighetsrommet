package no.nav.mulighetsrommet.arena.adapter.kafka

import no.nav.mulighetsrommet.arena.adapter.KafkaConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.getTopic
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository

class ConsumerSetup(kafkaConfig: KafkaConfig, eventRepository: EventRepository, apiClient: MulighetsrommetApiClient) {
    val consumers = listOf(
        TiltakEndretConsumer(kafkaConfig.getTopic("tiltakendret"), eventRepository, apiClient),
        TiltakgjennomforingEndretConsumer(kafkaConfig.getTopic("tiltakgjennomforingendret"), eventRepository, apiClient),
        TiltakdeltakerEndretConsumer(kafkaConfig.getTopic("tiltakdeltakerendret"), eventRepository, apiClient),
        SakEndretConsumer(kafkaConfig.getTopic("sakendret"), eventRepository, apiClient),
    )
}
