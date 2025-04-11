package no.nav.mulighetsrommet.api.utbetaling.pdl

import arrow.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory

class HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery(
    private val pdl: PdlClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun hentPersonOgGeografiskTilknytningBolk(
        identer: Set<PdlIdent>,
        accessType: AccessType,
    ): Either<PdlError, Map<PdlIdent, Pair<HentPersonBolkResponse.Person, GeografiskTilknytningResponse?>>> {
        val request = GraphqlRequest(
            query = $$"""
                query(identer: [ID!]!) {
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
                    hentGeografiskTilknytningBolk(identer: $identer) {
                        ident
                        geografiskTilknytning {
                            gtBydel
                            gtKommune
                            gtLand
                            gtType
                        }
                        code
                    }
                }
            """.trimIndent(),
            variables = GraphqlRequest.Identer(identer),
        )
        val response =
            pdl.graphqlRequest<GraphqlRequest.Identer, HentPersonOgGeografiskTilknytningBolkResponse>(
                request,
                accessType,
            )
                .getOrElse { return it.left() }

        val personBolkResponse = response.hentPersonBolk
            .mapNotNull {
                if (it.code == HentPersonBolkResponse.PersonBolkResponseEntry.Code.OK) {
                    val person = requireNotNull(it.person) {
                        "person forventet siden response var OK"
                    }
                    PdlIdent(it.ident) to person
                } else {
                    log.error("Response med ${it.code} fra pdl ved henting av person. Se secure logs for detaljer.")
                    SecureLog.logger.error("Response med ${it.code} fra pdl ved henting av person for ident=${it.ident}")
                    null
                }
            }.toMap()

        val geografiskTilknytningBolkResponse = response.hentGeografiskTilknytningBolk.mapNotNull {
            if (it.code == HentGeografiskTilknytningBolkResponseEntry.Code.OK) {
                val geografiskTilknytning = it.geografiskTilknytning.toGeografiskTilknytningResponse()

                PdlIdent(it.ident) to geografiskTilknytning
            } else {
                log.error("Response med ${it.code} fra pdl ved henting av geografisk tilknytning. Se secure logs for detaljer.")
                SecureLog.logger.error("Response med ${it.code} fra pdl ved henting av geografisk tilknytning for ident=${it.ident}")
                null
            }
        }.toMap()

        return personBolkResponse.mapValuesNotNull { (ident, value) -> value to geografiskTilknytningBolkResponse[ident] }
            .right()
    }

    @Serializable
    data class HentGeografiskTilknytningBolkResponseEntry(
        val ident: String,
        val geografiskTilknytning: PdlGeografiskTilknytning?,
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

    @Serializable
    data class HentPersonOgGeografiskTilknytningBolkResponse(
        val hentPersonBolk: List<HentPersonBolkResponse.PersonBolkResponseEntry>,
        val hentGeografiskTilknytningBolk: List<HentGeografiskTilknytningBolkResponseEntry>,
    )
}
