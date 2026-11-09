package no.nav.mulighetsrommet.api.domain.gjennomforing

import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.Tiltakskode

enum class Artskontokode {
    ARBEIDSFORBEREDENDE_TRENING,
    TILRETTELAGT_ARBEID_ORDINAER,
    VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    ARBEIDSRETTET_REHABILITERING,
    AVKLARING,
    DIGITALT_OPPFOLGINGSTILTAK,
    OPPFOLGING,
    JOBBKLUBB,
    ARBEIDSMARKEDSOPPLAERING,
    GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
    NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
    ENKELTPLASS_NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
    STUDIESPESIALISERING,
    ENKELTPLASS_STUDIESPESIALISERING,
    FAG_OG_YRKESOPPLAERING,
    GRUPPE_FAG_OG_YRKESOPPLAERING,
    ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
    HOYERE_YRKESFAGLIG_UTDANNING,
    HOYERE_UTDANNING,
}

fun utledArtskontokode(tiltakskode: Tiltakskode, oppstartstype: GjennomforingOppstartstype): Artskontokode? {
    val erEnkeltplass = oppstartstype == GjennomforingOppstartstype.ENKELTPLASS

    return when (tiltakskode) {
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> if (erEnkeltplass) {
            Artskontokode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
        } else {
            Artskontokode.ARBEIDSMARKEDSOPPLAERING
        }

        Tiltakskode.FAG_OG_YRKESOPPLAERING -> if (erEnkeltplass) {
            Artskontokode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING
        } else {
            Artskontokode.FAG_OG_YRKESOPPLAERING
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> if (erEnkeltplass) {
            Artskontokode.ENKELTPLASS_NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV
        } else {
            Artskontokode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV
        }

        Tiltakskode.STUDIESPESIALISERING -> if (erEnkeltplass) {
            Artskontokode.ENKELTPLASS_STUDIESPESIALISERING
        } else {
            Artskontokode.STUDIESPESIALISERING
        }

        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.AVKLARING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER,
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        Tiltakskode.INDIVIDUELL_JOBBSTOTTE,
        Tiltakskode.INDIVIDUELL_JOBBSTOTTE_UNG,
        Tiltakskode.ARBEID_MED_STOTTE,
        Tiltakskode.ARBEIDSTRENING,
        Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD,
        Tiltakskode.VARIG_LONNSTILSKUDD,
        Tiltakskode.MENTOR,
        Tiltakskode.INKLUDERINGSTILSKUDD,
        Tiltakskode.SOMMERJOBB,
        Tiltakskode.VTAO,
        Tiltakskode.FIREARIG_LONNSTILSKUDD,
        Tiltakskode.EKSPERTBISTAND,
        -> null
    }
}
