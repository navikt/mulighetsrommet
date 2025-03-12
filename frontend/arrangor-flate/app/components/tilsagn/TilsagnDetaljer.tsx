import { ArrangorflateTilsagn } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { formaterPeriode } from "~/utils";
import { Definisjonsliste, Definition } from "../Definisjonsliste";

interface Props {
  tilsagn: ArrangorflateTilsagn;
}

export function TilsagnDetaljer({ tilsagn }: Props) {
  const tilsagnDetaljer: Definition[] = [
    {
      key: "Tilsagnsperiode",
      value: formaterPeriode(tilsagn.periode),
    },
  ];

  const beregningDetaljer =
    tilsagn.beregning.type === "FORHANDSGODKJENT"
      ? [
          { key: "Antall plasser", value: String(tilsagn.beregning.input.antallPlasser) },
          { key: "Sats", value: formaterNOK(tilsagn.beregning.input.sats) },
          { key: "Beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
          { key: "Utbetalt så langt", value: "TODO" },
        ]
      : [
          { key: "Beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
          { key: "Utbetalt så langt", value: "TODO" },
        ];

  return (
    <Definisjonsliste className="mt-4" definitions={[...tilsagnDetaljer, ...beregningDetaljer]} />
  );
}
