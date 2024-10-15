import { type TilsagnDetaljer } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterTall } from "@mr/frontend-common/utils/utils";

interface Props {
  tilsagnsDetaljer: TilsagnDetaljer;
}

export function RefusjonTilsagnsDetaljer({ tilsagnsDetaljer }: Props) {
  const { antallPlasser, prisPerPlass, tilsagnsBelop, tilsagnsPeriode, sum } = tilsagnsDetaljer;
  return (
    <>
      <Definisjonsliste
        title="Tilsagnsdetaljer"
        definitions={[
          { key: "Antall avtalt plasser", value: String(antallPlasser) },
          { key: "Pris per plass", value: formaterTall(prisPerPlass) },
          { key: "Tilsagnsbeløp", value: formaterTall(tilsagnsBelop) },
          { key: "Tilsagnsperiode", value: String(tilsagnsPeriode) },
        ]}
      />
      <div className="flex justify-between mt-4 max-w-[50%]">
        <dt>Sum utbetalt så langt:</dt>
        <dd className="font-bold text-right">{formaterTall(sum)}</dd>
      </div>
    </>
  );
}
