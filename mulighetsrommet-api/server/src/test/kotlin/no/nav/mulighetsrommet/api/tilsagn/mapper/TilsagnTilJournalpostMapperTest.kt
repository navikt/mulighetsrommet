package no.nav.mulighetsrommet.api.tilsagn.mapper

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDateTime
import java.util.UUID

class TilsagnTilJournalpostMapperTest : FunSpec({
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val tilsagn = TilsagnJournalpostSnapshot(
        tilsagnId = UUID.fromString("72c45b92-4452-4b44-b1cd-9cfe7be86222"),
        arrangorOrganisasjonsnummer = Organisasjonsnummer("310438707"),
        arrangorNavn = "AKSEPTABEL EMPIRISK TIGER AS",
        tiltaksnummer = Tiltaksnummer("2025/11457"),
        besluttetTidspunkt = LocalDateTime.of(2026, 3, 1, 12, 0, 0),
    )

    test("mapping av tilsagn til journalpost inneholder ingen personopplysninger utover deltakers fødselsnummer") {
        val journalpost = TilsagnTilJournalpostMapper.tilJournalpost(
            tilsagn,
            pdf = "pdf-innhold".toByteArray(),
        )

        expectSelfie(jsonPrettyPrint.encodeToString(journalpost)).toMatchDisk("journalpost")
    }
})
