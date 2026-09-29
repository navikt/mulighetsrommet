package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.right
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafJournalpost

fun gyldigSafClient(): SafClient {
    val saf = mockk<SafClient> {
        coEvery { hentJournalpost(any(), any()) } returns SafJournalpost(journalpostId = "dummy", bruker = null).right()
    }
    return saf
}
