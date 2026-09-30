package no.nav.mulighetsrommet.api.tilskuddbehandling

import io.mockk.mockk
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator

fun gyldigJournalpostValidator(): JournalpostValidator = mockk(relaxed = true)
