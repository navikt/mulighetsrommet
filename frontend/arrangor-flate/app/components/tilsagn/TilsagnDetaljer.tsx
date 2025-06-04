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
          { key: "Totalt beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
          {
            key: "Gjenstående beløp",
            value: formaterNOK(tilsagn.gjenstaendeBelop),
          },
        ]
      : [
          { key: "Totalt beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
          {
            key: "Gjenstående beløp",
            value: formaterNOK(tilsagn.gjenstaendeBelop),
          },
        ];

  return (
    <Definisjonsliste
      className="p-4 border-1 border-border-divider rounded-lg"
      title={tilsagn.bestillingsnummer}
      definitions={[...tilsagnDetaljer, ...beregningDetaljer]}
    />
  );
}
