package no.nav.mulighetsrommet.api.endringshistorikk

enum class DocumentClass(val table: String) {
    AVTALE("avtale_endringshistorikk"),
    TILTAKSGJENNOMFORING("tiltaksgjennomforing_endringshistorikk"),
    TILSAGN("tilsagn_endringshistorikk"),
}
