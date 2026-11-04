package no.nav.mulighetsrommet.api.tilsagn.mapper

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.util.UUID

class TilsagnsbrevMeldingMapperTest : FunSpec({
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val tilsagn = TilsagnsbrevMeldingSnapshot(
        tilsagnId = UUID.fromString("72c45b92-4452-4b44-b1cd-9cfe7be86222"),
        tiltakstypeNavn = "Enkeltplass Arbeidsmarkedsopplæring",
        bestillingsnummer = "A-2026/1000-1",
        arrangorOrganisasjonsnummer = Organisasjonsnummer("310438707"),
        arrangorNavn = "AKSEPTABEL EMPIRISK TIGER AS",
        lopenummer = Tiltaksnummer("2025/11457")
    )

    val pdf = "pdf-innhold".toByteArray()

    test("mapper tilsagn til arrangør-melding uten personopplysninger") {
        val melding = TilsagnsbrevMeldingMapper.tilArrangorMelding(tilsagn, pdf)

        expectSelfie(jsonPrettyPrint.encodeToString(melding)).toMatchDisk()
    }
})
