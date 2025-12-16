package no.nav.mulighetsrommet.arena.adapter.clients

import arrow.core.Either
import io.ktor.client.plugins.ResponseException
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsFnr

interface ArenaOrdsProxyClient {
    suspend fun getArbeidsgiver(arbeidsgiverId: Int): Either<ResponseException, ArenaOrdsArrangor?>

    suspend fun getFnr(personId: Int): Either<ResponseException, ArenaOrdsFnr?>
}
