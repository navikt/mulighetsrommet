import { Refusjonskrav } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  krav: Refusjonskrav;
}

export function RefusjonDetaljer({ krav }: Props) {
  const { refusjonskravperiode } = krav.detaljer;

  return (
    <>
      <Definisjonsliste
        title="Refusjonskrav"
        definitions={[{ key: "Refusjonskravperiode", value: refusjonskravperiode }]}
      />
      <Definisjonsliste
        className="mt-4"
        definitions={[
          { key: "Antall månedsverk", value: String(krav.beregning.antallManedsverk) },
          { key: "Total refusjonskrav", value: String(krav.beregning.belop) },
        ]}
      />
    </>
  );
}
