package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

object TiltakstypeFixtures {
    val AFT = Tiltakstype(
        id = UUID.fromString("59a64a02-efdd-471d-9529-356ff5553a5d"),
        navn = "Arbeidsforberedende trening",
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arenakode = "ARBFORB",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val VTA = Tiltakstype(
        id = UUID.fromString("6fb921d6-0a87-4b8a-82a4-067477c1e113"),
        navn = "Varig tilrettelagt arbeid i skjermet virksomhet",
        tiltakskode = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        arenakode = "VASV",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val ArbeidsrettetRehabilitering = Tiltakstype(
        id = UUID.fromString("bc7128f9-3d5f-4190-a19a-ca392f17eb5c"),
        navn = "Arbeidsrettet rehabilitering",
        tiltakskode = Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        arenakode = "ARBRRHDAG",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val GruppeAmo = Tiltakstype(
        id = UUID.fromString("ca0cbc97-0306-4d7d-a368-10087e71c365"),
        navn = "Arbeidsmarkedsopplæring (Gruppe)",
        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        arenakode = "GRUPPEAMO",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val Oppfolging = Tiltakstype(
        id = UUID.fromString("5b827950-cf47-4716-9305-bcf7f2646a00"),
        navn = "Oppfølging",
        tiltakskode = Tiltakskode.OPPFOLGING,
        arenakode = "INDOPPFAG",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val VTAO = Tiltakstype(
        id = UUID.fromString("930ff2c1-2ab3-4787-aa9e-4006d26e8180"),
        navn = "Varig tilrettelagt arbeid i ordinær virksomhet",
        tiltakskode = Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER,
        arenakode = null,
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val Jobbklubb = Tiltakstype(
        id = UUID.fromString("801340cd-f0ae-4da9-a01c-caad692933a2"),
        navn = "Jobbklubb",
        tiltakskode = Tiltakskode.JOBBKLUBB,
        arenakode = "JOBBK",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val DigitalOppfolging = Tiltakstype(
        id = UUID.fromString("54300eac-a537-418e-a28b-9fa984a8d36f"),
        navn = "Digitalt jobbsøkerkurs",
        tiltakskode = Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        arenakode = "DIGIOPPARB",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val Avklaring = Tiltakstype(
        id = UUID.fromString("75c4587a-4d99-4924-935b-4244abb81d32"),
        navn = "Avklaring",
        tiltakskode = Tiltakskode.AVKLARING,
        arenakode = "AVKLARAG",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val GruppeFagOgYrkesopplaering = Tiltakstype(
        id = UUID.fromString("bcee8523-70e1-4253-9000-6a1430ef4326"),
        navn = "Fag- og yrkesopplæring (Gruppe)",
        tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        arenakode = "GRUFAGYRKE",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val Arbeidstrening = Tiltakstype(
        id = UUID.fromString("87cbc5c0-962e-4f34-93df-d78a887872a6"),
        navn = "Arbeidstrening",
        tiltakskode = Tiltakskode.ARBEIDSTRENING,
        arenakode = "ARBTREN",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val Amo = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "AMO",
        tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        arenakode = "GRUPPEAMO",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val NorskGrunnFOV = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "Norskopplæring, grunnleggende ferdigheter og FOV",
        tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        arenakode = "GRUPPEAMO",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val EnkelAmo = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "Enkel AMO",
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arenakode = "ENKELAMO",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val EnkelFagOgYrke = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "Fag- og yrkesopplæring eller fagskole (enkeltplass uten rammeavtale)",
        tiltakskode = Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        arenakode = "ENKFAGYRKE",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )

    val IPS = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "IPS",
        tiltakskode = Tiltakskode.INDIVIDUELL_JOBBSTOTTE,
        arenakode = "INDJOBSTOT",
        innsatsgrupper = emptySet(),
        sanityId = null,
    )
}
