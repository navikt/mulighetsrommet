package no.nav.mulighetsrommet.api.endringshistorikk

enum class DocumentClass(val table: String) {
    AVTALE("avtale_endringshistorikk"),
    TILTAKSGJENNOMFORING("gjennomforing_endringshistorikk"),
    TILSAGN("tilsagn_endringshistorikk"),
}
