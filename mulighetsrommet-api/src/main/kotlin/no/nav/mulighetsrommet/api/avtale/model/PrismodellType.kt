package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.model.Tiltakskode

enum class PrismodellType(val navn: String, val beskrivelse: List<String>) {
    ANNEN_AVTALT_PRIS(
        "Annen avtalt pris",
        listOf(
            "Prismodellen skal brukes når ingen av de andre valgene passer. Eksempelvis ved avtalt pris per deltaker per gjennomføring, fastpris eller avtaler med kombinasjon av for eksempel avtalt månedspris og fastpris.",
            "Arrangør benytter digital innsending og kan laste opp vedlegg som en del av innsendingen. Nav gjør ingen forhåndsberegning av beløp.",
        ),
    ),
    FORHANDSGODKJENT_PRIS_PER_MANEDSVERK(
        "Fast sats per tiltaksplass per måned",
        listOf(),
    ),
    AVTALT_PRIS_PER_MANEDSVERK(
        "Avtalt månedspris per tiltaksplass",
        listOf(
            "Nav beregner beløp til utbetaling ut fra den avtalte prisen som er lagt inn her, og registrerte deltakelser fra Deltakeroversikten.",
            "For deltakere som ikke har deltatt hele måneden, blir månedsverk beregnet ut fra antall hverdager bruker deltok på tiltaket, delt på det totale antallet hverdager i måneden.",
        ),
    ),
    AVTALT_PRIS_PER_UKESVERK(
        "Avtalt ukespris per tiltaksplass",
        listOf(
            "Nav beregner beløp til utbetaling ut fra den avtalte prisen som er lagt inn her, og registrerte deltakelser fra Deltakeroversikten.",
            "En dags deltakelse i tiltaket utgjør 0,2 ukesverk. En hel ukes deltakelse i tiltaket utgjør 1 ukesverk.",
        ),
    ),
    AVTALT_PRIS_PER_HELE_UKESVERK(
        "Avtalt pris per uke med påbegynt oppfølging per deltaker",
        listOf(
            "Beregningen brukes primært for tiltaket Digitalt jobbsøkerkurs.",
            "Systemet beregner tilsagns- og utbetalingsperioden til enten 4 eller 5 hele uker, ut fra hvordan ukedagene faller i måneden. " +
                "Nav beregner beløp til utbetaling ut fra den avtalte prisen som er lagt inn her, og registrerte deltakelser fra Deltakeroversikten. ",
            "Deltakere som deltar minst 1 dag i tiltaket i løpet av en uke, får beregnet en hel ukes deltakelse i tiltaket.",
        ),
    ),
    AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER(
        "Avtalt pris per time oppfølging per deltaker",
        listOf(
            "Arrangør sender inn krav via digital løsning, men må selv laste opp vedlegg som bekrefter benyttede timer per deltaker. " +
                "Nav gjør ingen forhåndsberegning av beløp. Arrangør ser registrerte deltakere i perioden fra Deltakeroversikten som en del av innsendingen.",
        ),
    ),
}

object Prismodeller {
    fun getPrismodellerForTiltak(tiltakskode: Tiltakskode): List<PrismodellType> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        -> listOf(
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        )

        Tiltakskode.AVKLARING,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        Tiltakskode.STUDIESPESIALISERING,
        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        -> listOf(
            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            PrismodellType.ANNEN_AVTALT_PRIS,
        )

        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        -> listOf()
    }
}
