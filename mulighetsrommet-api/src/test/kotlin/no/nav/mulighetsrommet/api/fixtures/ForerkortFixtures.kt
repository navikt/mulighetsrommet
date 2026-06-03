package no.nav.mulighetsrommet.api.fixtures

import kotliquery.Query
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse.Kode
import org.intellij.lang.annotations.Language
import java.util.UUID

object ForerkortFixtures {
    val A = ForerkortKlasse(
        id = UUID.fromString("810fe1c6-56b0-4e00-8ae6-00fb574299e5"),
        kode = Kode.A,
        navn = "A - Motorsykkel",
    )

    val A1 = ForerkortKlasse(
        id = UUID.fromString("c67006e4-2629-4993-a047-92f31b0db557"),
        kode = Kode.A1,
        navn = "A1 - Lett motorsykkel",
    )

    val A2 = ForerkortKlasse(
        id = UUID.fromString("ed44bd3a-aedb-4225-a3d8-c8f1b95fec5a"),
        kode = Kode.A2,
        navn = "A2 - Mellomtung motorsykkel",
    )

    val AM = ForerkortKlasse(
        id = UUID.fromString("dee7d6b8-02dc-4b7e-bb3a-fa71cc9248e3"),
        kode = Kode.AM,
        navn = "AM - Moped",
    )

    val AM_147 = ForerkortKlasse(
        id = UUID.fromString("ee66eb0b-d4a8-4527-800a-135dd3c0d422"),
        kode = Kode.AM_147,
        navn = "AM 147 - Mopedbil",
    )

    val B = ForerkortKlasse(
        id = UUID.fromString("79d1a970-e8f0-4ecd-8d5e-e7c8d5f3394c"),
        kode = Kode.B,
        navn = "B - Personbil",
    )

    val B_78 = ForerkortKlasse(
        id = UUID.fromString("84a40884-421c-406c-994d-4c4c15ef8bcc"),
        kode = Kode.B_78,
        navn = "B 78 - Personbil med automatgir",
    )

    val BE = ForerkortKlasse(
        id = UUID.fromString("cdbebefc-2cec-48d0-9c8e-bd464e56cfaa"),
        kode = Kode.BE,
        navn = "BE - Personbil med tilhenger",
    )

    val C = ForerkortKlasse(
        id = UUID.fromString("e3fcf1f7-1f20-4fca-bad5-422b7ee0418f"),
        kode = Kode.C,
        navn = "C - Lastebil",
    )

    val C1 = ForerkortKlasse(
        id = UUID.fromString("c65936e4-479f-4c84-b106-6c9ec0cf9aee"),
        kode = Kode.C1,
        navn = "C1 - Lett lastebil",
    )

    val C1E = ForerkortKlasse(
        id = UUID.fromString("69f88a08-e2de-461f-9258-4f8be546104a"),
        kode = Kode.C1E,
        navn = "C1E - Lett lastebil med tilhenger",
    )

    val CE = ForerkortKlasse(
        id = UUID.fromString("9a85cdeb-2f6d-44f6-bef2-2add850f7b27"),
        kode = Kode.CE,
        navn = "CE - Lastebil med tilhenger",
    )

    val D = ForerkortKlasse(
        id = UUID.fromString("e637320c-a5f0-4f7d-ad44-0a7c4654b4c2"),
        kode = Kode.D,
        navn = "D - Buss",
    )

    val D1 = ForerkortKlasse(
        id = UUID.fromString("5d890e23-6800-4574-a05d-24ca81f35a2a"),
        kode = Kode.D1,
        navn = "D1 - Minibuss",
    )

    val D1E = ForerkortKlasse(
        id = UUID.fromString("34d00562-f382-4027-953d-2b6f6bb7e0e5"),
        kode = Kode.D1E,
        navn = "D1E - Minibuss med tilhenger",
    )

    val DE = ForerkortKlasse(
        id = UUID.fromString("a7376d16-b0da-4140-8e67-c589be2c0ea2"),
        kode = Kode.DE,
        navn = "DE - Buss med tilhenger",
    )

    val S = ForerkortKlasse(
        id = UUID.fromString("5b1e1732-a5e8-45ca-955f-548c65d11065"),
        kode = Kode.S,
        navn = "S - Snøscooter",
    )

    val T = ForerkortKlasse(
        id = UUID.fromString("53896c05-7650-48ed-bf23-54ae78794eba"),
        kode = Kode.T,
        navn = "T - Traktor",
    )

    fun all() = setOf(
        A, A1, A2, AM, AM_147, B, B_78, BE, C, C1, C1E, CE, D, D1, D1E, DE, S, T,
    )

    fun query(): Query {
        val inserts = all().joinToString(",\n") { "('${it.id}','${it.kode.name}', '${it.navn}')" }

        @Language("PostgreSQL")
        val query = """
        insert into public.opplaring_forerkort (id, kode, navn)
            values $inserts
       on conflict (id) do nothing;
        """
        return queryOf(query)
    }
}
