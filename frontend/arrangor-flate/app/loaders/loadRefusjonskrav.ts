import { RefusjonskravService } from "@mr/api-client";
import { Refusjonskrav } from "~/domene/domene";

export async function loadRefusjonskrav(id: string): Promise<Refusjonskrav> {
  const krav = await RefusjonskravService.getRefusjonkrav({ id });

  const { beregning } = krav;

  return {
    id,
    detaljer: {
      tiltaksnavn: krav.gjennomforing.navn,
      tiltakstype: krav.tiltakstype.navn,
      refusjonskravperiode: `${beregning.periodeStart} - ${beregning.periodeSlutt}`,
    },
    beregning,
    deltakere: krav.deltakelser.map((d) => {
      const firstPeriode = d.perioder.at(0);
      const lastPeriode = d.perioder.at(-1);
      return {
        id: d.id,
        navn: d.navn,
        veileder: d.veileder,
        norskIdent: d.norskIdent,
        startDatoTiltaket: d.startDato,
        startDatoPerioden: firstPeriode?.start,
        sluttDatoPerioden: lastPeriode?.slutt,
        stillingsprosent: lastPeriode?.stillingsprosent,
        maanedsverk: d.manedsverk,
        perioder: d.perioder,
      };
    }),
  };
}
