package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.pdl.PdlNavn
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerPerson
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentPersonBolkResponse
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent

class DeltakerServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val norg2Client: Norg2Client = mockk(relaxed = true)
    val pdl: HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery = mockk(relaxed = true)

    afterEach {
        database.truncateAll()
    }

    fun createDeltakerService() = DeltakerService(
        db = database.db,
        pdlQuery = pdl,
        norg2Client = norg2Client,
    )

    val fnr1 = NorskIdent("12345678910")
    val fnr2 = NorskIdent("99887766554")
    val fnr3 = NorskIdent("01567712300")

    context("Henting av deltakere til kostnadsfordeling") {
        beforeTest {
            coEvery {
                pdl.hentPersonOgGeografiskTilknytningBolk(
                    any(),
                    any(),
                )
            } returns Either.Right(
                mapOf(
                    PdlIdent("12345678910") to Pair(
                        HentPersonBolkResponse.Person(
                            navn = listOf(
                                PdlNavn(fornavn = "Ola", etternavn = "Normann"),
                            ),
                            adressebeskyttelse = listOf(
                                HentPersonBolkResponse.Adressebeskyttelse(gradering = PdlGradering.UGRADERT),
                            ),
                            foedselsdato = listOf(
                                HentPersonBolkResponse.Foedselsdato(foedselsaar = 1980, foedselsdato = null),
                            ),
                        ),
                        GeografiskTilknytning.GtBydel(
                            value = "030102",
                        ),
                    ),
                    PdlIdent("99887766554") to Pair(
                        HentPersonBolkResponse.Person(
                            navn = listOf(
                                PdlNavn(fornavn = "Kari", etternavn = "Normann"),
                            ),
                            adressebeskyttelse = listOf(
                                HentPersonBolkResponse.Adressebeskyttelse(gradering = PdlGradering.STRENGT_FORTROLIG),
                            ),
                            foedselsdato = listOf(
                                HentPersonBolkResponse.Foedselsdato(foedselsaar = 1980, foedselsdato = null),
                            ),
                        ),
                        GeografiskTilknytning.GtBydel(
                            value = "030102",
                        ),
                    ),
                ),
            )
            coEvery {
                pdl.hentPersonOgGeografiskTilknytningBolk(
                    setOf(PdlIdent(fnr3.value)),
                    any(),
                )
            } returns Either.Right(
                mapOf(
                    PdlIdent("01567712300") to Pair(
                        HentPersonBolkResponse.Person(
                            navn = emptyList(),
                            adressebeskyttelse = listOf(
                                HentPersonBolkResponse.Adressebeskyttelse(gradering = PdlGradering.UGRADERT),
                            ),
                            foedselsdato = listOf(
                                HentPersonBolkResponse.Foedselsdato(foedselsaar = 1980, foedselsdato = null),
                            ),
                        ),
                        GeografiskTilknytning.GtBydel(
                            value = "030102",
                        ),
                    ),
                ),
            )
            coEvery { norg2Client.hentEnhetByGeografiskOmraade(any()) } returns Norg2EnhetDto(
                enhetId = 1,
                navn = "Nav Gjovik",
                enhetNr = NavEnhetNummer("0502"),
                status = Norg2EnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
            ).right()
        }

        test("Fjerner opplysninger på deltakere med adressebeskyttelse ved henting av deltakere") {
            val service = createDeltakerService()
            MulighetsrommetTestDomain().initialize(database.db)

            service.getDeltakereForKostnadsfordeling(listOf(fnr1, fnr2)) shouldBe mapOf(
                fnr1 to DeltakerPerson(
                    norskIdent = fnr1,
                    foedselsdato = null,
                    navn = "Normann, Ola",
                    geografiskEnhet = NavEnhetFixtures.Gjovik,
                    region = Innlandet,

                ),
                fnr2 to DeltakerPerson(
                    norskIdent = fnr2,
                    foedselsdato = null,
                    navn = "Adressebeskyttet person",
                    geografiskEnhet = null,
                    region = null,

                ),
            )
        }

        test("Håndterer deltakere som mangler navn fra PDL") {
            val service = createDeltakerService()
            MulighetsrommetTestDomain().initialize(database.db)

            service.getDeltakereForKostnadsfordeling(listOf(fnr3)) shouldBe mapOf(
                fnr3 to DeltakerPerson(
                    norskIdent = fnr3,
                    foedselsdato = null,
                    navn = "Ukjent",
                    geografiskEnhet = NavEnhetFixtures.Gjovik,
                    region = Innlandet,

                ),
            )
        }
    }
})
