package no.nav.mulighetsrommet.admin.personalia

import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn

fun AvvistGrunn.tilBrukertekst(): String = when (this) {
    AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE,
    AvvistGrunn.AVVIST_STRENGT_FORTROLIG_UTLAND,
    AvvistGrunn.AVVIST_FORTROLIG_ADRESSE,
    -> "Du har ikke tilgang til å se informasjon om denne personen fordi personen har adressebeskyttelse."

    AvvistGrunn.AVVIST_SKJERMING,
    -> "Du har ikke tilgang til å se informasjon om denne personen fordi personen er skjermet (egen ansatt)."

    AvvistGrunn.AVVIST_HABILITET,
    -> "Du har ikke tilgang til å se informasjon om denne personen på grunn av habilitet."

    AvvistGrunn.AVVIST_VERGE,
    AvvistGrunn.AVVIST_VERGEMAAL,
    -> "Du har ikke tilgang til å se informasjon om denne personen på grunn av vergemål."

    AvvistGrunn.AVVIST_AVDOED,
    -> "Du har ikke tilgang til å se informasjon fordi personen er død."

    AvvistGrunn.AVVIST_GEOGRAFISK,
    AvvistGrunn.AVVIST_PERSON_UTLAND,
    AvvistGrunn.AVVIST_UKJENT_BOSTED,
    -> "Du har ikke tilgang til å se informasjon om denne personen på grunn av geografisk tilknytning."
}
