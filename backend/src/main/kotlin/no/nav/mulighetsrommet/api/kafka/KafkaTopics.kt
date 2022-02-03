package no.nav.mulighetsrommet.api.kafka

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig

enum class KafkaTopics(val topic: String) {
    TiltaksgjennomforingEndret(HoconApplicationConfig(ConfigFactory.load()).property("ktor.kafka.topics.tiltaksgjennomforingEndret").toString()),
    TiltaksdeltakerEndret(HoconApplicationConfig(ConfigFactory.load()).property("ktor.kafka.topics.tiltaksdeltakerEndret").toString()),
    TiltaksgruppeEndret(HoconApplicationConfig(ConfigFactory.load()).property("ktor.kafka.topics.tiltaksgruppeEndret").toString()),
    TiltakEndret(HoconApplicationConfig(ConfigFactory.load()).property("ktor.kafka.topics.tiltakEndret").toString()),
    AvtaleinfoEndret(HoconApplicationConfig(ConfigFactory.load()).property("ktor.kafka.topics.avtaleinfoEndret").toString())
}
