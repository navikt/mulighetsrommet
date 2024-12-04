package no.nav.mulighetsrommet.api.pdfgen

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravAft
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.pdf.createPDFA
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider

object Pdfgen {
    init {
        VeraGreenfieldFoundryProvider.initialise()
        PDFGenCore.init(Environment())
    }

    private val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        registerKotlinModule()
    }

    object Aft {
        fun refusjonKvittering(refusjonKravAft: RefusjonKravAft, tilsagn: List<ArrangorflateTilsagn>): ByteArray {
            @Serializable
            data class PdfData(
                val refusjon: RefusjonKravAft,
                val tilsagn: List<ArrangorflateTilsagn>,
            )

            val jsonNode: JsonNode = objectMapper.valueToTree(PdfData(refusjonKravAft, tilsagn))
            return createPDFA("aft-refusjon-kvittering", "refusjon", jsonNode)
                ?: throw Exception("Kunne ikke generere PDF")
        }

        fun refusjonJournalpost(refusjonKravAft: RefusjonKravAft, tilsagn: List<ArrangorflateTilsagn>): ByteArray {
            @Serializable
            data class PdfData(
                val refusjon: RefusjonKravAft,
                val tilsagn: List<ArrangorflateTilsagn>,
            )

            val jsonNode: JsonNode = objectMapper.valueToTree(PdfData(refusjonKravAft, tilsagn))
            requireNotNull(jsonNode)
            return createPDFA("aft-refusjon-journalpost", "refusjon", jsonNode)
                ?: throw Exception("Kunne ikke generere PDF")
        }
    }
}
