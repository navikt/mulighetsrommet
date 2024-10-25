import { ArrangorflateTilsagn } from "@mr/api-client";
import { Definisjonsliste, Definition } from "../Definisjonsliste";
import { formaterDato } from "~/utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

interface Props {
  tilsagn: ArrangorflateTilsagn;
}

export function TilsagnDetaljer({ tilsagn }: Props) {
  const tilsagnDetaljer: Definition[] = [
    {
      key: "Tilsagnsperiode",
      value: `${formaterDato(tilsagn.periodeStart)} - ${formaterDato(tilsagn.periodeSlutt)}`,
    },
  ];

  const beregningDetaljer =
    tilsagn.beregning.type === "AFT"
      ? [
          { key: "Antall plasser", value: String(tilsagn.beregning.antallPlasser) },
          { key: "Sats", value: formaterNOK(tilsagn.beregning.sats) },
          { key: "Beløp", value: formaterNOK(tilsagn.beregning.belop) },
          { key: "Utbetalt så langt", value: "TODO" },
        ]
      : [
          { key: "Beløp", value: formaterNOK(tilsagn.beregning.belop) },
          { key: "Utbetalt så langt", value: "TODO" },
        ];

  return (
    <Definisjonsliste className="mt-4" definitions={[...tilsagnDetaljer, ...beregningDetaljer]} />
  );
}
