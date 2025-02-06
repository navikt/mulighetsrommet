import { ArrFlateRefusjonKrav } from "@mr/api-client-v2";
import { formaterDato } from "../../utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { BeregningDetaljer } from "./BeregningDetaljer";

interface Props {
  krav: ArrFlateRefusjonKrav;
}

export function RefusjonDetaljer({ krav }: Props) {
  return (
    <>
      <Definisjonsliste
        title="Refusjonskrav"
        definitions={[
          {
            key: "Refusjonskravperiode",
            value: `${formaterDato(krav.periodeStart)} - ${formaterDato(krav.periodeSlutt)}`,
          },
          { key: "Frist for innsending", value: formaterDato(krav.fristForGodkjenning) },
        ]}
      />
      <BeregningDetaljer beregning={krav.beregning} />
    </>
  );
}
