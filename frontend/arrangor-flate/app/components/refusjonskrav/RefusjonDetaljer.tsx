import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterDato } from "../../utils";
import { RefusjonKravAft } from "@mr/api-client-v2";

interface Props {
  krav: RefusjonKravAft;
}

export function RefusjonDetaljer({ krav }: Props) {
  return (
    <>
      <Definisjonsliste
        title="Refusjonskrav"
        definitions={[
          {
            key: "Refusjonskravperiode",
            value: `${formaterDato(krav.beregning.periodeStart)} - ${formaterDato(krav.beregning.periodeSlutt)}`,
          },
          { key: "Frist for innsending", value: formaterDato(krav.fristForGodkjenning) },
        ]}
      />
      <Definisjonsliste
        className="mt-4"
        definitions={[
          { key: "Antall månedsverk", value: String(krav.beregning.antallManedsverk) },
          { key: "Total refusjonskrav", value: formaterNOK(krav.beregning.belop) },
        ]}
      />
    </>
  );
}
