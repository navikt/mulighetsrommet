import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ArrangorflateTilsagn } from "api-client";
import { formaterPeriode } from "~/utils";
import { Definisjonsliste, Definition } from "../Definisjonsliste";

interface Props {
  tilsagn: ArrangorflateTilsagn;
  ekstraDefinisjoner?: Definition[];
}

export function TilsagnDetaljer({ tilsagn, ekstraDefinisjoner }: Props) {
  const tilsagnDetaljer: Definition[] = [
    ...(ekstraDefinisjoner || []),
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
          {
            key: "Utbetalt så langt",
            value: formaterNOK(tilsagn.beregning.output.belop - tilsagn.gjenstaendeBelop),
          },
        ]
      : [
          { key: "Beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
          {
            key: "Utbetalt så langt",
            value: formaterNOK(tilsagn.beregning.output.belop - tilsagn.gjenstaendeBelop),
          },
        ];

  return (
    <Definisjonsliste className="mt-4" definitions={[...tilsagnDetaljer, ...beregningDetaljer]} />
  );
}
