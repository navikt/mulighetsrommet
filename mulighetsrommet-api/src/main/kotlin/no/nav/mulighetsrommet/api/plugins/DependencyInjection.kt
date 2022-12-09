package no.nav.mulighetsrommet.api.plugins

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.server.application.*
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.KafkaConfig
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClientImpl
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClientImpl
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClientImpl
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClientImpl
import no.nav.mulighetsrommet.api.clients.veileder.VeilarbveilederClient
import no.nav.mulighetsrommet.api.clients.veileder.VeilarbveilederClientImpl
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.services.*
import no.nav.mulighetsrommet.api.services.kafka.KafkaProducerService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    install(Koin) {
        SLF4JLogger()

        modules(
            db(appConfig.database),
            repositories(),
            services(
                appConfig,
                veilarbvedsstotte(appConfig),
                veilarboppfolging(appConfig),
                veilarbperson(appConfig),
                veilarbdialog(appConfig),
                veilarbveileder(appConfig),
                amtenhetsregister(appConfig)
            )
        )
    }
}

private fun db(config: DatabaseConfig): Module {
    return module(createdAtStart = true) {
        single<Database> {
            FlywayDatabaseAdapter(config)
        }
    }
}

private fun kafka(kafkaConfig: KafkaConfig): KafkaProducerService<String, String> {
    if (NaisEnv.current().isLocal()) {
        return KafkaProducerService(
            KafkaProducerClientBuilder.builder<String, String>()
                .withProperties(
                    KafkaPropertiesBuilder.producerBuilder()
                        .withBrokerUrl(kafkaConfig.brokerUrl)
                        .withBaseProperties()
                        .withProducerId(kafkaConfig.producerId)
                        .withSerializers(StringSerializer::class.java, StringSerializer::class.java)
                        .build()
                )
                .build()
        )
    }

    return KafkaProducerService(
        KafkaProducerClientBuilder.builder<String, String>()
            .withProperties(KafkaPropertiesPreset.aivenDefaultProducerProperties(kafkaConfig.producerId))
            .build()
    )
}

private fun veilarbvedsstotte(config: AppConfig): VeilarbvedtaksstotteClient {
    return VeilarbvedtaksstotteClientImpl(
        config.veilarbvedtaksstotteConfig.url,
        { accessToken ->
            tokenClientProvider(config).exchangeOnBehalfOfToken(
                config.veilarbvedtaksstotteConfig.scope,
                accessToken
            )
        }
    )
}

private fun veilarboppfolging(config: AppConfig): VeilarboppfolgingClient {
    return VeilarboppfolgingClientImpl(
        config.veilarboppfolgingConfig.url,
        { accessToken ->
            tokenClientProvider(config).exchangeOnBehalfOfToken(
                config.veilarboppfolgingConfig.scope,
                accessToken
            )
        }
    )
}

private fun veilarbperson(config: AppConfig): VeilarbpersonClient {
    return VeilarbpersonClientImpl(
        config.veilarbpersonConfig.url,
        { accessToken ->
            tokenClientProvider(config).exchangeOnBehalfOfToken(
                config.veilarbpersonConfig.scope,
                accessToken
            )
        }

    )
}

private fun veilarbdialog(config: AppConfig): VeilarbdialogClient {
    return VeilarbdialogClientImpl(
        config.veilarbdialogConfig.url,
        { accessToken ->
            tokenClientProvider(config).exchangeOnBehalfOfToken(
                config.veilarbdialogConfig.scope,
                accessToken
            )
        }
    )
}

private fun veilarbveileder(config: AppConfig): VeilarbveilederClient {
    return VeilarbveilederClientImpl(
        config.veilarbveilederConfig.url,
        { accessToken ->
            tokenClientProvider(config).exchangeOnBehalfOfToken(
                config.veilarbveilederConfig.scope,
                accessToken
            )
        }

    )
}

private fun amtenhetsregister(config: AppConfig): AmtEnhetsregisterClient {
    return AmtEnhetsregisterClientImpl(
        baseUrl = config.amtEnhetsregister.url,
        machineToMachineTokenClient = {
            tokenClientProviderForMachineToMachine(config).createMachineToMachineToken(
                config.amtEnhetsregister.scope
            )
        }
    )
}

private fun tokenClientProvider(config: AppConfig): AzureAdOnBehalfOfTokenClient {
    return when (NaisEnv.current().isLocal()) {
        true -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.auth.azure.audience)
            .withPrivateJwk(createRSAKeyForLokalUtvikling("azure").toJSONString())
            .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
            .buildOnBehalfOfTokenClient()

        false -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildOnBehalfOfTokenClient()
    }
}

private fun tokenClientProviderForMachineToMachine(config: AppConfig): MachineToMachineTokenClient {
    return when (NaisEnv.current().isLocal()) {
        true -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.auth.azure.audience)
            .withPrivateJwk(createRSAKeyForLokalUtvikling("azure").toJSONString())
            .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()

        false -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildMachineToMachineTokenClient()
    }
}

private fun repositories() = module {
    single { TiltaksgjennomforingRepository(get()) }
    single { TiltakstypeRepository(get()) }
    single { DeltakerRepository(get()) }
}

private fun services(
    appConfig: AppConfig,
    veilarbvedsstotte: VeilarbvedtaksstotteClient,
    veilarboppfolging: VeilarboppfolgingClient,
    veilarbpersonClient: VeilarbpersonClient,
    veilarbdialogClient: VeilarbdialogClient,
    veilarbveilerClient: VeilarbveilederClient,
    amtEnhetsregisterClient: AmtEnhetsregisterClient
) = module {
    val m2mTokenProvider = tokenClientProviderForMachineToMachine(appConfig)

    single { ArenaService(get(), get(), get()) }
    single { HistorikkService(get(), get()) }
    single { SanityService(appConfig.sanity, get()) }
    single { ArrangorService(amtEnhetsregisterClient) }
    single {
        BrukerService(
            veilarboppfolgingClient = veilarboppfolging,
            veilarbvedtaksstotteClient = veilarbvedsstotte,
            veilarbpersonClient = veilarbpersonClient
        )
    }
    single { DialogService(veilarbdialogClient) }
    single {
        AnsattService(
            veilarbveilederClient = veilarbveilerClient,
            poaoTilgangService = get()
        )
    }
    single {
        val poaoTilgangClient = PoaoTilgangHttpClient(
            appConfig.poaoTilgang.url,
            { m2mTokenProvider.createMachineToMachineToken(appConfig.poaoTilgang.scope) }
        )
        PoaoTilgangService(poaoTilgangClient)
    }
    single { DelMedBrukerService(get()) }
    single { kafka(appConfig.kafka) }
}

private fun createRSAKeyForLokalUtvikling(keyID: String): RSAKey = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
    }
