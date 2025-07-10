import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ArrangorflateTilsagn } from "api-client";
import { Definisjonsliste, Definition } from "../common/Definisjonsliste";
import { tekster } from "~/tekster";
import { formaterPeriode } from "~/utils/date";

interface Props {
  tilsagn: ArrangorflateTilsagn;
  ekstraDefinisjoner?: Definition[];
  headingLevel?: "3" | "4";
}

export function TilsagnDetaljer({ tilsagn, headingLevel, ekstraDefinisjoner }: Props) {
  const tilsagnDetaljer: Definition[] = [
    ...(ekstraDefinisjoner || []),
    { key: "Tilsagnstype", value: tekster.bokmal.tilsagn.tilsagntype(tilsagn.type) },
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
      headingLevel={headingLevel ?? "3"}
      className="p-4 border-1 border-border-divider rounded-lg w-xl"
      title={`${tekster.bokmal.tilsagn.tilsagntype(tilsagn.type)} - ${tilsagn.bestillingsnummer}`}
      definitions={[...tilsagnDetaljer, ...beregningDetaljer]}
    />
  );
}
