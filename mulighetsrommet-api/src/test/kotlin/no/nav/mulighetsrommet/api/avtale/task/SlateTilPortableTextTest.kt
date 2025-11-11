package no.nav.mulighetsrommet.api.avtale.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.FaneinnholdLenke
import no.nav.mulighetsrommet.model.PortableTextTypedObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.List

class SlateTilPortableTextTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createTask() = SlateTilPortableText(
        database.db,
    )

    val slateTextList = Json.decodeFromString<List<PortableTextTypedObject>>(
        """
        [ { "_key": null, "_type": "block", "children": [ { "text": "Målgruppe:", "_type": "span", "marks": ["strong"] } ], "markDefs": [] }, { "_key": null, "_type": "block", "children": [ { "text": "Arbeidssøkeren skal ikke ha rett på norskopplæring etter introduksjonsloven eller integreringsloven.", "_type": "span" } ], "listItem": "bullet", "markDefs": [] }, { "_key": null, "_type": "block", "children": [ { "text": "Arbeidssøkerne må ha språknivå A1-A2 ved oppstart til kurs.", "_type": "span" } ], "listItem": "bullet", "markDefs": [] }, { "_key": null, "_type": "block", "children": [{ "text": "", "_type": "span" }], "markDefs": [] }, { "_key": null, "_type": "block", "children": [ { "text": "HK-dir sin ", "_type": "span" }, { "text": "nettside", "_type": "span", "marks": ["https://prove.hkdir.no/norskprove-a1-b2"] }, { "text": " inneholder nyttig informasjon om språknivå og norskprøver. Her finner du nærmere beskrivelse av alle språknivåene. I tillegg er det øvelsesoppgaver som kan gjøre det lettere for deltaker og Nav-veileder å finne ut hvilket nivå deltaker ligger på.", "_type": "span" } ], "markDefs": [ { "_key": "https://prove.hkdir.no/norskprove-a1-b2", "href": "https://prove.hkdir.no/norskprove-a1-b2", "_type": "link" } ] } ]
        """.trimIndent(),
    )

    val faneinnhold = Faneinnhold(
        forHvem = slateTextList,
        detaljerOgInnhold = slateTextList,
        pameldingOgVarighet = slateTextList,
        kontaktinfo = slateTextList,
        oppskrift = slateTextList,
        forHvemInfoboks = null,
        detaljerOgInnholdInfoboks = null,
        pameldingOgVarighetInfoboks = null,
        kontaktinfoInfoboks = null,
        lenker = null,
        delMedBruker = null,
    )

    val avtale1 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        startDato = LocalDate.of(2025, 5, 1),
        sluttDato = LocalDate.of(2025, 5, 31),
        status = AvtaleStatusType.AKTIV,
        faneinnhold = faneinnhold,
    )
    val avtale2 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        startDato = LocalDate.of(2025, 5, 1),
        sluttDato = LocalDate.of(2025, 6, 30),
        status = AvtaleStatusType.AKTIV,
        faneinnhold = faneinnhold,
    )

    val domain = MulighetsrommetTestDomain(
        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
        avtaler = listOf(avtale1, avtale2),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    test("skal mappe faneinnhold fra slate til portable text") {
        database.run {
            val assertFaneinnhold = { avtale: Avtale ->
                val serialized = Json.encodeToString(avtale.faneinnhold)
                serialized.shouldNotContain("null")
            }

            val task = createTask()

            task.execute()

            database.run {
                queries.avtale.get(avtale1.id).shouldNotBeNull().should {
                    assertFaneinnhold(it)
                }
                queries.avtale.get(avtale2.id).shouldNotBeNull().should {
                    assertFaneinnhold(it)
                }
            }
        }
    }
})
