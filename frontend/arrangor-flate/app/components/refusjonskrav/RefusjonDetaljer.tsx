import { formaterTall } from "@mr/frontend-common/utils/utils";
import { Refusjonskrav } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterDato } from "~/utils";

interface Props {
  krav: Refusjonskrav;
}

export function RefusjonDetaljer({ krav }: Props) {
  const { refusjonskravperiode } = krav.detaljer;
  const [start, end] = refusjonskravperiode.split(" - ");

  return (
    <>
      <Definisjonsliste
        title="Refusjonskrav"
        definitions={[
          { key: "Refusjonskravperiode", value: `${formaterDato(start)} - ${formaterDato(end)}` },
        ]}
      />
      <Definisjonsliste
        className="mt-4"
        definitions={[
          { key: "Antall månedsverk", value: String(krav.beregning.antallManedsverk) },
          { key: "Total refusjonskrav", value: formaterTall(krav.beregning.belop) },
        ]}
      />
    </>
  );
}
