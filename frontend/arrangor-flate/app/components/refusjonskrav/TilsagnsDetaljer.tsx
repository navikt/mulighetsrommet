import { type TilsagnDetaljer } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

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
          { key: "Tilsagnsbeløp", value: formaterNOK(tilsagnsBelop) },
          { key: "Tilsagnsperiode", value: String(tilsagnsPeriode) },
          { key: "Antall avtalt plasser", value: String(antallPlasser) },
          { key: "Pris per plass", value: formaterNOK(prisPerPlass) },
        ]}
      />
      <div className="flex justify-between mt-4 max-w-[50%]">
        <dt>Sum utbetalt så langt:</dt>
        <dd className="font-bold text-right">{formaterNOK(sum)}</dd>
      </div>
    </>
  );
}
