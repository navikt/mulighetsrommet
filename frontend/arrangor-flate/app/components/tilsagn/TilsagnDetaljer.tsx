import { ArrangorflateTilsagn, TilsagnBeregningAFT } from "@mr/api-client";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterDato } from "~/utils";
import { formaterTall } from "@mr/frontend-common/utils/utils";

interface Props {
  tilsagn: ArrangorflateTilsagn;
}

export function TilsagnDetaljer({ tilsagn }: Props) {
  return (
    <>
      <Definisjonsliste
        className="mt-4"
        definitions={[
          {
            key: "Tilsagnsperiode",
            value: `${formaterDato(tilsagn.periodeStart)} - ${formaterDato(tilsagn.periodeSlutt)}`,
          },
          { key: "Beløp", value: formaterTall(tilsagn.beregning.belop) },
          { key: "Utbetalt så langt", value: formaterTall(0) },
        ]}
      />
      {tilsagn.beregning.type === "AFT" ? (
        <AFTDetaljer beregning={tilsagn.beregning} />
      ) : (
        <div>Feil tilsagnstype</div>
      )}
    </>
  );
}

function AFTDetaljer({ beregning }: { beregning: TilsagnBeregningAFT }) {
  return (
    <Definisjonsliste
      className="mt-4"
      definitions={[
        { key: "Antall plasser", value: String(beregning.antallPlasser) },
        { key: "Sats", value: formaterTall(beregning.sats) },
      ]}
    />
  );
}
