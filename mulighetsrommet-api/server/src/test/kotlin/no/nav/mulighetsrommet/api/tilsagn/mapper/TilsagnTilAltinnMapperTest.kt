package no.nav.mulighetsrommet.api.tilsagn.mapper

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.altinn.AltinnAttachmentInitRequest
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceRequest
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.Instant
import java.util.UUID

class TilsagnTilAltinnMapperTest : FunSpec({
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val tilsagn = TilsagnAltinnSnapshot(
        tiltakstypeNavn = "Enkeltplass Arbeidsmarkedsopplæring",
        bestillingsnummer = "A-2026/9999-1",
        arrangorOrganisasjonsnummer = Organisasjonsnummer("310438707"),
        arrangorNavn = "AKSEPTABEL EMPIRISK TIGER AS",
    )

    val vedleggId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val idempotentKey = UUID.fromString("72c45b92-4452-4b44-b1cd-9cfe7be86222")
    val now = Instant.parse("2026-03-01T12:00:00Z")

    test("mapping av tilsagn til altinn-vedlegg og -korrespondanse inneholder ingen personopplysninger") {
        val vedlegg = TilsagnTilAltinnMapper.tilAltinnVedlegg(tilsagn, "pdf-innhold".toByteArray())
        val korrespondanse = TilsagnTilAltinnMapper.tilAltinnKorrespondanse(tilsagn, vedleggId, idempotentKey, now)

        expectSelfie(
            jsonPrettyPrint.encodeToString(Resultat(vedlegg, korrespondanse)),
        ).toMatchDisk("tilsagnsbrevAltinn")
    }
})

@Serializable
private data class Resultat(
    val vedlegg: AltinnAttachmentInitRequest,
    val korrespondanse: AltinnCorrespondenceRequest,
)
