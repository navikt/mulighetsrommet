package no.nav.mulighetsrommet.arena.adapter

import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig

val ApplicationConfigDev = Config(
    server = ServerConfig(
        port = 8084,
        host = "0.0.0.0",
    ),
    app = AppConfig(
        enableFailedRecordProcessor = true,
        tasks = TaskConfig(
            retryFailedEvents = RetryFailedEvents.Config(
                delayOfMinutes = 15,
            ),
            notifyFailedEvents = NotifyFailedEvents.Config(
                maxRetries = 5,
                cron = "0 0 7 * * MON-FRI",
            ),
        ),
        services = ServiceConfig(
            mulighetsrommetApi = ServiceClientConfig(
                url = "http://mulighetsrommet-api",
                scope = "api://dev-gcp.team-mulighetsrommet.mulighetsrommet-api/.default",
            ),
            tiltakshistorikk = ServiceClientConfig(
                url = "http://tiltakshistorikk",
                scope = "api://dev-gcp.team-mulighetsrommet.tiltakshistorikk/.default",
            ),
            arenaEventService = ArenaEventService.Config(
                channelCapacity = 10000,
                numChannelConsumers = 100,
                maxRetries = 5,
            ),
            arenaOrdsProxy = ServiceClientConfig(
                url = "https://amt-arena-ords-proxy.dev-fss-pub.nais.io/api",
                scope = "api://dev-fss.amt.amt-arena-ords-proxy/.default",
            ),
        ),
        database = DatabaseConfig(
            jdbcUrl = System.getenv("DB_JDBC_URL"),
            maximumPoolSize = 10,
        ),
        flyway = FlywayMigrationManager.MigrationConfig(),
        kafka = KafkaConfig(
            consumerPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties("mulighetsrommet-kafka-consumer.v1"),
            consumers = KafkaConsumers(
                arenaTiltakEndret = KafkaTopicConsumer.Config(
                    id = "arena-tiltakstype-endret",
                    topic = "teamarenanais.aapen-arena-tiltakendret-v1-q2",
                ),
                arenaTiltakgjennomforingEndret = KafkaTopicConsumer.Config(
                    id = "arena-tiltakgjennomforing-endret",
                    topic = "teamarenanais.aapen-arena-tiltakgjennomforingendret-v1-q2",
                ),
                arenaTiltakdeltakerEndret = KafkaTopicConsumer.Config(
                    id = "arena-tiltakdeltaker-endret",
                    topic = "teamarenanais.aapen-arena-tiltakdeltakerendret-v1-q2",
                ),
                arenaHistTiltakdeltakerEndret = KafkaTopicConsumer.Config(
                    id = "arena-hist-tiltakdeltaker-endret",
                    topic = "teamarenanais.aapen-arena-histtiltakdeltakerendret-v1-q2",
                ),
                arenaSakEndret = KafkaTopicConsumer.Config(
                    id = "arena-sakendret-endret",
                    topic = "teamarenanais.aapen-arena-sakendret-v1-q2",
                ),
                arenaAvtaleInfoEndret = KafkaTopicConsumer.Config(
                    id = "arena-avtaleinfo-endret",
                    topic = "teamarenanais.aapen-arena-avtaleinfoendret-v1-q2",
                ),
            ),
        ),
        auth = AuthConfig(
            azure = AuthProvider(
                issuer = System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
                jwksUri = System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
                audience = System.getenv("AZURE_APP_CLIENT_ID"),
                tokenEndpointUrl = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                privateJwk = System.getenv("AZURE_APP_JWK"),
            ),
        ),
        slack = SlackConfig(
            token = System.getenv("SLACK_TOKEN"),
            channel = "#team-valp-monitorering-dev",
            enable = true,
        ),
    ),
)
