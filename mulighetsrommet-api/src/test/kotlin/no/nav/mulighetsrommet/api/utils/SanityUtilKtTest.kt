package no.nav.mulighetsrommet.api.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class SanityUtilKtTest : FunSpec({

    test("should get tiltaksgjennomføring by id") {
        val originalQuery =
            """*[_type == \"tiltaksgjennomforing\" && !(_id in path(\"drafts.**\"))
                 && tiltakstype->innsatsgruppe->tittel in [\"Situasjonsbestemt innsats\", \"Spesielt tilpasset innsats\"]
                 && tiltakstype->_id in [\"3526de0d-ad4c-4b81-b072-a13b3a4b4ed3\"]
                 %ENHET%
                 ]
                 {
                   _id,
                   tiltaksgjennomforingNavn,
                   lokasjon,
                   oppstart
                   oppstartsdato
                   tiltaksnummer
                   kontaktinfoArrangor->
                   tiltakstype->
                 }
              """
        val expectedQuery =
            """*[_type == \"tiltaksgjennomforing\" && !(_id in path(\"drafts.**\"))
                 && tiltakstype->innsatsgruppe->tittel in [\"Situasjonsbestemt innsats\", \"Spesielt tilpasset innsats\"]
                 && tiltakstype->_id in [\"3526de0d-ad4c-4b81-b072-a13b3a4b4ed3\"]
                 && (("0219" in enheter[]->nummer.current) || (enheter[0] == null && "0600" == fylke->nummer.current))
                 ]
                 {
                   _id,
                   tiltaksgjennomforingNavn,
                   lokasjon,
                   oppstart
                   oppstartsdato
                   tiltaksnummer
                   kontaktinfoArrangor->
                   tiltakstype->
                 }
              """
        replaceEnhetInQuery(
            query = originalQuery,
            "0219",
            "0600"
        ) shouldBe expectedQuery
    }
})
