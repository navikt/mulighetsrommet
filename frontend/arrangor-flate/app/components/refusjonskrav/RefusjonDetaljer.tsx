import { Krav } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  krav: Krav;
}

export function RefusjonDetaljer({ krav }: Props) {
  const { kravnr, periode, belop } = krav;

  return (
    <>
      <Definisjonsliste
        title="Refusjonskrav"
        definitions={[
          { key: "Refusjonskravnummer", value: kravnr },
          { key: "Refusjonskravperiode", value: periode },
        ]}
      />
      <Definisjonsliste
        className="mt-4"
        definitions={[
          { key: "Antall månedsverk", value: "15.27" },
          { key: "Total refusjonskrav", value: belop },
        ]}
      />
    </>
  );
}
