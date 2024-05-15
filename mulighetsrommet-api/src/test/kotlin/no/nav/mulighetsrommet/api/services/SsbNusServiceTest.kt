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
                                code = "30",
                                parentCode = "3",
                                level = "2",
                                name = "Almenne fag",
                                shortName = null,
                            ),
                            ClassificationItem(
                                code = "31",
                                parentCode = "3",
                                level = "2",
                                name = "Humanistiske og estetiske fag",
                                shortName = null,
                            ),
                            ClassificationItem(
                                code = "32",
                                parentCode = "3",
                                level = "2",
                                name = "Lærerutdanninger og utdanninger i pedagogikk",
                                shortName = null,
                            ),
                            ClassificationItem(
                                code = "355",
                                parentCode = "35",
                                level = "4",
                                name = "Utdanninger i elektrofag, mekaniske fag og maskinfag",
                                shortName = null,
                            ),
                            ClassificationItem(
                                code = "3551",
                                parentCode = "355",
                                level = "4",
                                name = "Elektro",
                                shortName = null,
                            ),
                            ClassificationItem(
                                code = "3552",
                                parentCode = "355",
                                level = "4",
                                name = "Mekaniske fag",
                                shortName = null,
                            ),
                            ClassificationItem(
                                code = "357",
                                parentCode = "35",
                                level = "4",
                                name = "Bygg- og anleggsfag",
                                shortName = null,
                            ),
                            ClassificationItem(
                                code = "3571",
                                parentCode = "357",
                                level = "4",
                                name = "Bygg og anlegg",
                                shortName = null,
                            ),
                        ),
                    ),
                    version = "2437",
                )

                @Language("PostgreSQL")
                val query = """
             insert into tiltakstype_nus_kodeverk(tiltakskode, code, version)
             values ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '30', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '31', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '32', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '355', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3551', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3552', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '357', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3571', '2437')
                """.trimIndent()
                database.db.run(queryOf(query).asExecute)
            }

            test("getData skal returnere korrekt format på dataene") {
                val result = service.getNusData(Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING, "2437")
                result shouldBeEqual mapOf(
                    "Almenne fag" to listOf(createNusElement("30", "Almenne fag", "3")),
                    "Humanistiske og estetiske fag" to listOf(
                        createNusElement(
                            "31",
                            "Humanistiske og estetiske fag",
                            "3",
                        ),
                    ),
                    "Lærerutdanninger og utdanninger i pedagogikk" to listOf(
                        createNusElement(
                            "32",
                            "Lærerutdanninger og utdanninger i pedagogikk",
                            "3",
                        ),
                    ),
                    "Utdanninger i elektrofag, mekaniske fag og maskinfag" to listOf(
                        createNusElement("3551", "Elektro", "355"),
                        createNusElement("3552", "Mekaniske fag", "355"),
                        createNusElement("355", "Utdanninger i elektrofag, mekaniske fag og maskinfag", "35"),
                    ),
                    "Bygg- og anleggsfag" to listOf(
                        createNusElement("3571", "Bygg og anlegg", "357"),
                        createNusElement("357", "Bygg- og anleggsfag", "35"),
                    ),
                )
            }
        }
    },
)

private fun createNusElement(code: String, name: String, parent: String): NusElement {
    return NusElement(
        tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        code = code,
        name = name,
        parent = parent,
        version = "2437",
    )
}
