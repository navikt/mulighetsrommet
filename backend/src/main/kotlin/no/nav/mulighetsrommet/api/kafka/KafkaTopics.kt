package no.nav.mulighetsrommet.api.kafka

enum class KafkaTopics(val topic: String) {
    TiltaksgjennomforingEndret("teamarenanais.aapen-arena-tiltakgjennomforingendret-v1-q2"),
    TiltaksdeltakerEndret("teamarenanais.aapen-arena-tiltakdeltakerendret-v1-q2"),
    TiltaksgruppeEndret("teamarenanais.aapen-arena-tiltaksgruppeendret-v1-q2"),
    TiltakEndret("teamarenanais.aapen-arena-tiltakendret-v1-q2"),
    AvtaleinfoEndret("teamarenanais.aapen-arena-avtaleinfoendret-v1-q2")
}
