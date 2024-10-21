import { ArrangorflateService } from "@mr/api-client";
import { Refusjonskrav } from "~/domene/domene";
import { formaterDato } from "~/utils";

export async function loadRefusjonskrav(id: string): Promise<Refusjonskrav> {
  const krav = await ArrangorflateService.getRefusjonkrav({ id });

  const { beregning } = krav;

  const deltakere = krav.deltakelser.map((d) => {
    const firstPeriode = d.perioder.at(0);
    const lastPeriode = d.perioder.at(-1);
    return {
      id: d.id,
      person: d.person,
      veileder: d.veileder,
      startDatoTiltaket: d.startDato,
      startDatoPerioden: firstPeriode?.start,
      sluttDatoPerioden: lastPeriode?.slutt,
      stillingsprosent: lastPeriode?.stillingsprosent,
      maanedsverk: d.manedsverk,
      perioder: d.perioder,
    };
  });

  return {
    id,
    detaljer: {
      tiltaksnavn: krav.gjennomforing.navn,
      tiltakstype: krav.tiltakstype.navn,
      refusjonskravperiode: `${formaterDato(beregning.periodeStart)} - ${formaterDato(beregning.periodeSlutt)}`,
    },
    beregning,
    deltakere,
  };
}
