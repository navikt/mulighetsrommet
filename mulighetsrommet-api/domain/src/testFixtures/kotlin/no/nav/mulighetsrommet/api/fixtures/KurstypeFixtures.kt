package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import java.util.UUID

object KurstypeFixtures {
    val norskopplaering = Kurstype(
        UUID.fromString("8e294221-bf60-466a-96bd-7c59c338ee5e"),
        kode = Kurstype.Kode.NORSKOPPLAERING,
        navn = "Norskopplæring",
    )
    val grunnleggendeFerdigheter = Kurstype(
        id = UUID.fromString("19544ff4-25e5-4925-b942-6109b2a98552"),
        kode = Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER,
        navn = "Grunnleggende ferdigheter",
    )
    val fov = Kurstype(
        UUID.fromString("347ef4a1-be8c-47b6-8e67-54244b648a9f"),
        kode = Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
        navn = "FOV (Forberedende opplæring for voksne)",
    )
    val bransjeOgYrkesrettet = Kurstype(
        id = UUID.fromString("8c439235-4363-4137-859e-bfa33b0e8f2d"),
        kode = Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
        navn = "Bransje og yrkesrettet",
    )
    val studiespesialisering = Kurstype(
        id = UUID.fromString("a262e282-2f81-411d-b450-06b7f3d371dc"),
        kode = Kurstype.Kode.STUDIESPESIALISERING,
        navn = "Studiespesialisering",
    )

    fun all() = setOf(norskopplaering, grunnleggendeFerdigheter, fov, bransjeOgYrkesrettet, studiespesialisering)
}
