package no.nav.mulighetsrommet.api.services

import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysV2ClientImpl
import no.nav.common.client.axsys.CachedAxsysClient
import no.nav.mulighetsrommet.api.ServiceClientConfig

class AxsysService(config: ServiceClientConfig, token: () -> String) {
    private val client = AxsysV2ClientImpl(config.url, token)

    fun get(): AxsysClient {
        return CachedAxsysClient(client)
    }
}
