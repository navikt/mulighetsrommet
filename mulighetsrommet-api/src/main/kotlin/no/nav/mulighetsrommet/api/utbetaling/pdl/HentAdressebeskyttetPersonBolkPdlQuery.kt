package no.nav.mulighetsrommet.api.utbetaling.pdl

import arrow.core.Either
import arrow.core.NonEmptySet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.LocalDate

class HentAdressebeskyttetPersonBolkPdlQuery(
    private val pdl: PdlClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun hentPersonBolk(identer: NonEmptySet<PdlIdent>): Either<PdlError, Map<PdlIdent, HentPersonBolkResponse.Person>> {
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
                            foedselsdato {
                                foedselsdato
                                foedselsaar
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
                                PdlIdent(it.ident) to person
                            }

                            else -> {
                                log.error("Response med ${it.code} fra pdl. Se secure logs for detaljer.")
                                SecureLog.logger.error("Response med ${it.code} fra pdl for ident=${it.ident}")
                                null
                            }
                        }
                    }
                    .toMap()
            }
    }
}

@Serializable
data class HentPersonBolkResponse(
    val hentPersonBolk: List<PersonBolkResponseEntry>,
) {
    @Serializable
    data class Person(
        val navn: List<PdlNavn>,
        val adressebeskyttelse: List<Adressebeskyttelse>,
        val foedselsdato: List<Foedselsdato>,
    )

    @Serializable
    data class Adressebeskyttelse(
        val gradering: PdlGradering = PdlGradering.UGRADERT,
    )

    @Serializable
    data class Foedselsdato(
        val foedselsaar: Int,
        @Serializable(with = LocalDateSerializer::class)
        val foedselsdato: LocalDate?,
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
