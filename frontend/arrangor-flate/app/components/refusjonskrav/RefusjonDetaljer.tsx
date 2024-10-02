import { Krav } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  krav: Krav;
}

export function RefusjonDetaljer({ krav }: Props) {
  const { periode, belop } = krav;

  return (
    <>
      <Definisjonsliste
        title="Refusjonskrav"
        definitions={[{ key: "Refusjonskravperiode", value: periode }]}
      />
      <Definisjonsliste
        className="mt-4"
        definitions={[
          { key: "Antall månedsverk", value: "todo" },
          { key: "Total refusjonskrav", value: belop },
        ]}
      />
    </>
  );
}
