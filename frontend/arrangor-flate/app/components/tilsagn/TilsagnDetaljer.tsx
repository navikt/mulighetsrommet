import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ArrangorflateTilsagn } from "api-client";
import { formaterPeriode } from "~/utils";
import { Definisjonsliste, Definition } from "../Definisjonsliste";
import { tekster } from "~/tekster";

interface Props {
  tilsagn: ArrangorflateTilsagn;
  ekstraDefinisjoner?: Definition[];
}

export function TilsagnDetaljer({ tilsagn, ekstraDefinisjoner }: Props) {
  const tilsagnDetaljer: Definition[] = [
    ...(ekstraDefinisjoner || []),
    { key: "Tilsagnstype", value: tekster.bokmal.tilsagn.tilsagntype(tilsagn.type) },
    {
      key: "Tilsagnsperiode",
      value: formaterPeriode(tilsagn.periode),
    },
  ];

  const beregningDetaljer = getTilsagnBeregningDetaljer(tilsagn);

  return (
    <Definisjonsliste
      className="p-4 border-1 border-border-divider rounded-lg w-xl"
      title={`${tekster.bokmal.tilsagn.tilsagntype(tilsagn.type)} - ${tilsagn.bestillingsnummer}`}
      definitions={[...tilsagnDetaljer, ...beregningDetaljer]}
    />
  );
}

function getTilsagnBeregningDetaljer(tilsagn: ArrangorflateTilsagn) {
  switch (tilsagn.beregning.type) {
    case "PRIS_PER_MANEDSVERK":
      return [
        { key: "Antall plasser", value: String(tilsagn.beregning.input.antallPlasser) },
        { key: "Sats", value: formaterNOK(tilsagn.beregning.input.sats) },
        { key: "Totalt beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
        {
          key: "Gjenstående beløp",
          value: formaterNOK(tilsagn.gjenstaendeBelop),
        },
      ];
    case "FRI":
      return [
        { key: "Totalt beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
        {
          key: "Gjenstående beløp",
          value: formaterNOK(tilsagn.gjenstaendeBelop),
        },
      ];
  }
}
