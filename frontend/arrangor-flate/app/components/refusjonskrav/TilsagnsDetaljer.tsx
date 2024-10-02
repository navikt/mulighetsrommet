import { type TilsagnsDetaljer } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  tilsagnsDetaljer: TilsagnsDetaljer;
}

export function RefusjonTilsagnsDetaljer({ tilsagnsDetaljer }: Props) {
  const { antallPlasser, prisPerPlass, tilsagnsBelop, tilsagnsPeriode, sum } = tilsagnsDetaljer;
  return (
    <>
      <Definisjonsliste
        title="Tilsagnsdetaljer"
        definitions={[
          { key: "Antall avtalt plasser", value: String(antallPlasser) },
          { key: "Pris per plass", value: String(prisPerPlass) },
          { key: "Tilsagnsbeløp", value: String(tilsagnsBelop) },
          { key: "Tilsagnsperiode", value: String(tilsagnsPeriode) },
        ]}
      />
      <div className="flex justify-between mt-4 max-w-[50%]">
        <dt>Sum utbetalt så langt:</dt>
        <dd className="font-bold text-right">{sum}</dd>
      </div>
    </>
  );
}
