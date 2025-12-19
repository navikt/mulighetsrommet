package no.nav.mulighetsrommet.api.utbetaling.pdl

import arrow.core.Either
import arrow.core.NonEmptySet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.pdl.Adressebeskyttelse
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.pdl.PdlNavn
import no.nav.mulighetsrommet.api.clients.pdl.tilGradering
import no.nav.mulighetsrommet.api.clients.pdl.tilNavn
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.teamLogsError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory

class HentAdressebeskyttetPersonBolkPdlQuery(
    private val pdl: PdlClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun hentPersonBolk(identer: NonEmptySet<PdlIdent>): Either<PdlError, Map<PdlIdent, PdlPerson>> {
        val request = GraphqlRequest(
            query = $$"""
                query($identer: [ID!]!) {
                    hentPersonBolk(identer: $identer) {
                        ident
                        person {
                            navn {
                                fornavn
                                mellomnavn
                                etternavn
                            }
                            adressebeskyttelse {
                                gradering
                            }
                        }
                        code
                    }
                }
            """.trimIndent(),
            variables = GraphqlRequest.Identer(identer),
        )
        return pdl
            .graphqlRequest<GraphqlRequest.Identer, HentPersonBolkResponse>(request, AccessType.M2M)
            .map { response ->
                response.hentPersonBolk
                    .mapNotNull {
                        when (it.code) {
                            HentPersonBolkResponse.PersonBolkResponseEntry.Code.OK -> {
                                val person = requireNotNull(it.person) {
                                    "person forventet siden response var OK"
                                }
                                PdlIdent(it.ident) to PdlPerson(
                                    navn = person.navn.tilNavn(),
                                    gradering = person.adressebeskyttelse.tilGradering(),
                                )
                            }

                            else -> {
                                log.error("Response med ${it.code} fra pdl. Se Team Logs for detaljer.")
                                SecureLog.logger.error("Response med ${it.code} fra pdl for ident=${it.ident}")
                                log.teamLogsError("Response med ${it.code} fra pdl for ident=${it.ident}")
                                null
                            }
                        }
                    }
                    .toMap()
            }
    }
}

data class PdlPerson(
    val navn: String?,
    val gradering: PdlGradering,
)

@Serializable
data class HentPersonBolkResponse(
    val hentPersonBolk: List<PersonBolkResponseEntry>,
) {
    @Serializable
    data class Person(
        val navn: List<PdlNavn>,
        val adressebeskyttelse: List<Adressebeskyttelse>,
    )

    @Serializable
    data class PersonBolkResponseEntry(
        val ident: String,
        val person: Person?,
        val code: Code,
    ) {

        enum class Code {
            @SerialName("ok")
            OK,

            /**
             * Fant ikke person.
             */
            @SerialName("not_found")
            NOT_FOUND,

            /**
             * Ugyldig ident.
             */
            @SerialName("bad_request")
            BAD_REQUEST,
        }
    }
}
