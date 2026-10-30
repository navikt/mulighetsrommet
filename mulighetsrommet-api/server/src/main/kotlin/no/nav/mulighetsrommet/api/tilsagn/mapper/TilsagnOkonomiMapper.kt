package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.Artskonto
import no.nav.tiltak.okonomi.Regnskapskonto

object TilsagnOkonomiMapper {
    fun toRegnskapskonto(type: TilsagnType): Regnskapskonto = when (type) {
        TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN -> Regnskapskonto.TILTAK_DRIFTSTILSKUDD
        TilsagnType.INVESTERING -> Regnskapskonto.TILTAK_INVESTERINGER
        TilsagnType.TILSKUDD_TIL_OPPLAERING -> Regnskapskonto.TILTAK_OPPLAERING_TILSKUDD
    }

    fun toArtskonto(tiltakskode: Tiltakskode): Artskonto = when (tiltakskode) {
        Tiltakskode.ARBEIDSRETTET_REHABILITERING -> Artskonto.ARBEIDSRETTET_REHABILITERING
        Tiltakskode.AVKLARING -> Artskonto.AVKLARING
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> Artskonto.DIGITALT_OPPFOLGINGSTILTAK
        Tiltakskode.JOBBKLUBB -> Artskonto.JOBBKLUBB
        Tiltakskode.OPPFOLGING -> Artskonto.OPPFOLGING
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> Artskonto.ARBEIDSFORBEREDENDE_TRENING
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> Artskonto.VARIG_TILRETTELAGT_ARBEID_SKJERMET
        Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER -> Artskonto.TILRETTELAGT_ARBEID_ORDINAER
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> Artskonto.ARBEIDSMARKEDSOPPLAERING
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING -> Artskonto.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING -> Artskonto.ENKELTPLASS_FAG_OG_YRKESOPPLAERING
        Tiltakskode.FAG_OG_YRKESOPPLAERING -> Artskonto.FAG_OG_YRKESOPPLAERING
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> Artskonto.GRUPPE_ARBEIDSMARKEDSOPPLAERING
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> Artskonto.GRUPPE_FAG_OG_YRKESOPPLAERING
        Tiltakskode.HOYERE_UTDANNING -> Artskonto.HOYERE_UTDANNING
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING -> Artskonto.HOYERE_YRKESFAGLIG_UTDANNING
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> Artskonto.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV
        Tiltakskode.STUDIESPESIALISERING -> Artskonto.STUDIESPESIALISERING
        else -> error("Artskonto for $tiltakskode er ikke støttet")
    }
}
