package no.nav.mulighetsrommet.admin.testing

import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.admin.tiltak.AvtaleDto
import no.nav.mulighetsrommet.admin.tiltak.PrismodellDto
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import java.time.LocalDate
import java.util.UUID

object AvtaleDtoFixtures {
    fun createAvtaleDto(
        id: UUID = UUID.randomUUID(),
        tiltakstype: AvtaleDto.Tiltakstype = AvtaleDto.Tiltakstype(
            id = TiltakstypeFixtures.Oppfolging.id,
            navn = TiltakstypeFixtures.Oppfolging.navn,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        ),
        navn: String = "Avtale hos Fretex",
        avtalenummer: String? = "2024/1",
        sakarkivNummer: SakarkivNummer? = null,
        arrangor: AvtaleDto.ArrangorHovedenhet? = null,
        startDato: LocalDate = LocalDate.of(2024, 1, 1),
        sluttDato: LocalDate? = null,
        avtaletype: Avtaletype = Avtaletype.AVTALE,
        status: AvtaleDto.Status = AvtaleDto.Status(
            type = AvtaleStatusType.AKTIV,
            status = DataElement.Status("Aktiv", DataElement.Status.Variant.SUCCESS),
        ),
        administratorer: List<AvtaleDto.Administrator> = emptyList(),
        kontorstruktur: List<Kontorstruktur> = emptyList(),
        beskrivelse: String? = null,
        faneinnhold: Faneinnhold? = null,
        personopplysninger: List<Personopplysning> = emptyList(),
        personvernBekreftet: Boolean = false,
        opplaring: OpplaringKategoriseringDetaljer? = null,
        opsjonsmodell: Opsjonsmodell = Opsjonsmodell(
            type = OpsjonsmodellType.INGEN_OPSJONSMULIGHET,
            opsjonMaksVarighet = null,
        ),
        opsjonerRegistrert: List<Avtale.OpsjonLogg> = emptyList(),
        prismodeller: List<PrismodellDto> = emptyList(),
    ) = AvtaleDto(
        id = id,
        tiltakstype = tiltakstype,
        navn = navn,
        avtalenummer = avtalenummer,
        sakarkivNummer = sakarkivNummer,
        arrangor = arrangor,
        startDato = startDato,
        sluttDato = sluttDato,
        avtaletype = avtaletype,
        status = status,
        administratorer = administratorer,
        kontorstruktur = kontorstruktur,
        beskrivelse = beskrivelse,
        faneinnhold = faneinnhold,
        personopplysninger = personopplysninger,
        personvernBekreftet = personvernBekreftet,
        opplaring = opplaring,
        opsjonsmodell = opsjonsmodell,
        opsjonerRegistrert = opsjonerRegistrert,
        prismodeller = prismodeller,
    )
}
