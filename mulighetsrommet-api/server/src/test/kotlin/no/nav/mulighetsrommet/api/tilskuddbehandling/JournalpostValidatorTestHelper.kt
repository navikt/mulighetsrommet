package no.nav.mulighetsrommet.api.tilskuddbehandling

import no.nav.mulighetsrommet.admin.journalpost.FormatJournalpostValidator
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator

fun gyldigJournalpostValidator(): JournalpostValidator = FormatJournalpostValidator
