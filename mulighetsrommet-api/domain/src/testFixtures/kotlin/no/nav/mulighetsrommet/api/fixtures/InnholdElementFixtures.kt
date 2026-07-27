package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import java.util.UUID

object InnholdElementFixtures {
    val grunnleggendeFerdigheter =
        InnholdElement(
            id = UUID.fromString("312a02cd-8330-4c32-ae4b-8e9cde0060fb"),
            kode = InnholdElement.Kode.GRUNNLEGGENDE_FERDIGHETER,
            navn = "Grunnleggende ferdigheter",
        )
    val teoretiskOpplaring =
        InnholdElement(
            id = UUID.fromString("0770648f-210e-4b2e-9524-0b87226c8f4b"),
            kode = InnholdElement.Kode.TEORETISK_OPPLAERING,
            navn = "Teoretisk opplæring",
        )
    val jobbsokerKompetanse =
        InnholdElement(
            id = UUID.fromString("7ee66328-2dd0-4180-9bb3-165e7854f3b6"),
            kode = InnholdElement.Kode.JOBBSOKER_KOMPETANSE,
            navn = "Jobbsøkerkompetanse",
        )
    val praksis =
        InnholdElement(
            id = UUID.fromString("4264da96-3b68-426a-8629-37a2fafafa27"),
            kode = InnholdElement.Kode.PRAKSIS,
            navn = "Praksis",
        )
    val arbeidsmarkedskunnskap =
        InnholdElement(
            id = UUID.fromString("8f729892-dbb0-448e-ac4c-7169464f955c"),
            kode = InnholdElement.Kode.ARBEIDSMARKEDSKUNNSKAP,
            navn = "Arbeidsmarkedskunnskap",
        )
    val norskopplaring =
        InnholdElement(
            id = UUID.fromString("c645e920-618c-4dcf-a3b7-516f32040e04"),
            kode = InnholdElement.Kode.NORSKOPPLAERING,
            navn = "Norskopplæring",
        )
    val bransjerettetOpplaring =
        InnholdElement(
            id = UUID.fromString("331d61f7-c957-4d9c-a229-41d1d1b9c675"),
            kode = InnholdElement.Kode.BRANSJERETTET_OPPLARING,
            navn = "Bransjerettet opplæring",
        )

    fun all() = setOf(
        grunnleggendeFerdigheter,
        teoretiskOpplaring,
        jobbsokerKompetanse,
        praksis,
        arbeidsmarkedskunnskap,
        norskopplaring,
        bransjerettetOpplaring,
    )
}
