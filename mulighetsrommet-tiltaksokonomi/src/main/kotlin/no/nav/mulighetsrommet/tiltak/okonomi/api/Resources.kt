package no.nav.mulighetsrommet.tiltak.okonomi.api

import io.ktor.resources.*

private const val API_BASE_PATH = "/api/v1/okonomi"

@Resource("$API_BASE_PATH/bestilling")
class Bestilling {

    @Resource("{id}")
    class Id(val parent: Bestilling = Bestilling(), val id: String) {

        @Resource("status")
        class Status(val parent: Id)
    }
}

@Resource("$API_BASE_PATH/faktura")
class Faktura {

    @Resource("{id}")
    class Id(val parent: Faktura = Faktura(), val id: String)
}
