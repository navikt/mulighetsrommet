import { ArrangorflateService } from "@mr/api-client";
import { Refusjonskrav } from "~/domene/domene";
import { formaterDato } from "~/utils";

export async function loadRefusjonskrav(id: string): Promise<Refusjonskrav> {
  const krav = await ArrangorflateService.getRefusjonkrav({ id });

  const { beregning } = krav;

  return {
    id,
    detaljer: {
      tiltaksnavn: krav.gjennomforing.navn,
      tiltakstype: krav.tiltakstype.navn,
      refusjonskravperiode: `${formaterDato(beregning.periodeStart)} - ${formaterDato(beregning.periodeSlutt)}`,
    },
    beregning,
    betalingsinformasjon: krav.betalingsinformasjon,
    deltakere: krav.deltakelser,
  };
}
