package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.ssb.ClassificationItem
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusClient
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusData
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.repositories.NusElement
import no.nav.mulighetsrommet.api.repositories.SsbNusRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.intellij.lang.annotations.Language

class SsbNusServiceTest : FunSpec(
    {
        val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

        context("SsbNusService") {
            val client: SsbNusClient = mockk(relaxed = true)
            val repository = SsbNusRepository(database.db)
            val service = SsbNusService(client, repository)

            val domain = MulighetsrommetTestDomain()

            beforeEach {
                domain.initialize(database.db)
                repository.upsert(
                    data = SsbNusData(
                        validFrom = "",
                        classificationItems = listOf(
                            ClassificationItem(
                                code = "3",
                                parentCode = null,
                                level = "1",
                                name = "Videregående, grunnutdanning",
                            ),
                            ClassificationItem(
                                code = "30",
                                parentCode = "3",
                                level = "2",
                                name = "Allmenne fag",
                            ),
                            ClassificationItem(
                                code = "31",
                                parentCode = "3",
                                level = "2",
                                name = "Humanistiske og estetiske fag",
                            ),
                            ClassificationItem(
                                code = "32",
                                parentCode = "3",
                                level = "2",
                                name = "Lærerutdanninger og utdanninger i pedagogikk",
                            ),
                            ClassificationItem(
                                code = "35",
                                parentCode = "3",
                                level = "2",
                                name = "Naturvitenskapelige fag, håndverksfag og tekniske fag",
                            ),
                            ClassificationItem(
                                code = "355",
                                parentCode = "35",
                                level = "4",
                                name = "Utdanninger i elektrofag, mekaniske fag og maskinfag",
                            ),
                            ClassificationItem(
                                code = "3551",
                                parentCode = "355",
                                level = "4",
                                name = "Elektro",
                            ),
                            ClassificationItem(
                                code = "3552",
                                parentCode = "355",
                                level = "4",
                                name = "Mekaniske fag",
                            ),
                            ClassificationItem(
                                code = "357",
                                parentCode = "35",
                                level = "4",
                                name = "Bygg- og anleggsfag",
                            ),
                            ClassificationItem(
                                code = "3571",
                                parentCode = "357",
                                level = "4",
                                name = "Bygg og anlegg",
                            ),
                            ClassificationItem(
                                code = "4",
                                parentCode = null,
                                level = "1",
                                name = "Videregående, avsluttende utdanning",
                            ),
                            ClassificationItem(
                                code = "40",
                                parentCode = "4",
                                level = "2",
                                name = "Allmenne fag",
                            ),
                            ClassificationItem(
                                code = "41",
                                parentCode = "4",
                                level = "2",
                                name = "Humanistiske og estetiske fag",
                            ),
                            ClassificationItem(
                                code = "42",
                                parentCode = "4",
                                level = "2",
                                name = "Lærerutdanninger og utdanninger i pedagogikk",
                            ),
                            ClassificationItem(
                                code = "45",
                                parentCode = "4",
                                level = "2",
                                name = "Naturvitenskapelige fag, håndverksfag og tekniske fag",
                            ),
                            ClassificationItem(
                                code = "455",
                                parentCode = "45",
                                level = "4",
                                name = "Utdanninger i elektrofag, mekaniske fag og maskinfag",
                            ),
                            ClassificationItem(
                                code = "4551",
                                parentCode = "455",
                                level = "4",
                                name = "Elektro",
                            ),
                            ClassificationItem(
                                code = "4552",
                                parentCode = "455",
                                level = "4",
                                name = "Mekaniske fag",
                            ),
                            ClassificationItem(
                                code = "457",
                                parentCode = "45",
                                level = "4",
                                name = "Bygg- og anleggsfag",
                            ),
                            ClassificationItem(
                                code = "4571",
                                parentCode = "457",
                                level = "4",
                                name = "Bygg og anlegg",
                            ),
                        ),
                    ),
                    version = "2437",
                )

                @Language("PostgreSQL")
                val query = """
             insert into tiltakstype_nus_kodeverk(tiltakskode, code, version)
             values
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '30', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '31', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '32', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '35', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3551', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3552', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3571', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '40', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '41', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '42', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '45', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4551', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4552', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4571', '2437')
                """.trimIndent()
                database.db.run(queryOf(query).asExecute)
            }

            test("getData skal returnere korrekt format på dataene") {
                val result = service.getNusData(Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING, "2437")
                result shouldBeEqual NusDataResponse(
                    data = listOf(
                        NusData(
                            nivaa = "Videregående, grunnutdanning",
                            kategorier = listOf(
                                createNusElement("30", "Allmenne fag", "3", "2"),
                                createNusElement(
                                    "31",
                                    "Humanistiske og estetiske fag",
                                    "3",
                                    "2",
                                ),
                                createNusElement(
                                    "32",
                                    "Lærerutdanninger og utdanninger i pedagogikk",
                                    "3",
                                    "2",

                                ),

                                createNusElement(
                                    "35",
                                    "Naturvitenskapelige fag, håndverksfag og tekniske fag",
                                    "3",
                                    "2",
                                ),
                                createNusElement("3551", "Elektro", "355", "4"),
                                createNusElement("3552", "Mekaniske fag", "355", "4"),
                                createNusElement("3571", "Bygg og anlegg", "357", "4"),
                            ),
                        ),
                        NusData(
                            nivaa = "Videregående, avsluttende utdanning",
                            kategorier = listOf(
                                createNusElement("40", "Allmenne fag", "4", "2"),
                                createNusElement(
                                    "41",
                                    "Humanistiske og estetiske fag",
                                    "4",
                                    "2",
                                ),
                                createNusElement(
                                    "42",
                                    "Lærerutdanninger og utdanninger i pedagogikk",
                                    "4",
                                    "2",
                                ),
                                createNusElement(
                                    "45",
                                    "Naturvitenskapelige fag, håndverksfag og tekniske fag",
                                    "4",
                                    "2",
                                ),
                                createNusElement("4551", "Elektro", "455", "4"),
                                createNusElement("4552", "Mekaniske fag", "455", "4"),
                                createNusElement("4571", "Bygg og anlegg", "457", "4"),
                            ),
                        ),
                    ),
                )
            }
        }
    },
)

private fun createNusElement(code: String, name: String, parent: String, level: String): NusElement {
    return NusElement(
        tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        code = code,
        version = "2437",
        name = name,
        parent = parent,
        level = level,
    )
}
