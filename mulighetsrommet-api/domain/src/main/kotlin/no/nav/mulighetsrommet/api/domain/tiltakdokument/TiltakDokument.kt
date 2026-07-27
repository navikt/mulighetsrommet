package no.nav.mulighetsrommet.api.domain.tiltakdokument

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

data class TiltakDokument(
    val id: UUID,
    val navn: String,
    val sanityId: UUID?,
    val tiltaksnummer: String?,
    val tiltakstypeId: UUID,
    val stedForGjennomforing: String?,
    val arrangorId: UUID?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val publisert: Boolean,
    val administratorer: List<NavIdent>,
    val navEnheter: List<NavEnhetNummer>,
    val kontaktpersoner: List<Kontaktperson>,
    val arrangorKontaktpersoner: List<UUID>,
) {
    @Serializable
    data class Kontaktperson(
        val navIdent: NavIdent,
        val beskrivelse: String?,
    )
}
