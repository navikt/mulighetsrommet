import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ArrangorflateTilsagn } from "api-client";
import { Definisjonsliste, Definition } from "../common/Definisjonsliste";
import { tekster } from "~/tekster";
import { formaterPeriode } from "~/utils/date";
import { TilsagnStatusTag } from "./TilsagnStatusTag";

interface Props {
  tilsagn: ArrangorflateTilsagn;
  minimal?: boolean;
  headingLevel?: "3" | "4";
}

export function TilsagnDetaljer({ tilsagn, headingLevel, minimal = false }: Props) {
  const tilsagnDetaljer: Definition[] = [
    ...(!minimal
      ? [
          { key: "Status", value: <TilsagnStatusTag data={tilsagn.status} /> },
          { key: "Tiltakstype", value: tilsagn.tiltakstype.navn },
          { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
        ]
      : []),
    { key: "Tilsagnsperiode", value: formaterPeriode(tilsagn.periode) },
  ];

  const beregningDetaljer = getTilsagnBeregningDetaljer(tilsagn);

  return (
    <Definisjonsliste
      headingLevel={headingLevel ?? "3"}
      className="p-4 border-1 border-border-divider rounded-lg w-xl"
      title={`${tekster.bokmal.tilsagn.tilsagntype(tilsagn.type)} ${tilsagn.bestillingsnummer}`}
      definitions={[...tilsagnDetaljer, ...beregningDetaljer]}
    />
  );
}

function getTilsagnBeregningDetaljer(tilsagn: ArrangorflateTilsagn) {
  switch (tilsagn.beregning.type) {
    case "FRI":
      return [
        { key: "Totalt beløp", value: formaterNOK(tilsagn.beregning.belop) },
        { key: "Gjenstående beløp", value: formaterNOK(tilsagn.gjenstaendeBelop) },
      ];
    case "PRIS_PER_MANEDSVERK":
      return [
        { key: "Antall plasser", value: String(tilsagn.beregning.antallPlasser) },
        { key: "Pris per månedsverk", value: formaterNOK(tilsagn.beregning.sats) },
        { key: "Totalt beløp", value: formaterNOK(tilsagn.beregning.belop) },
        { key: "Gjenstående beløp", value: formaterNOK(tilsagn.gjenstaendeBelop) },
      ];
    case "PRIS_PER_UKESVERK":
      return [
        { key: "Antall plasser", value: String(tilsagn.beregning.antallPlasser) },
        { key: "Pris per ukesverk", value: formaterNOK(tilsagn.beregning.sats) },
        { key: "Totalt beløp", value: formaterNOK(tilsagn.beregning.belop) },
        { key: "Gjenstående beløp", value: formaterNOK(tilsagn.gjenstaendeBelop) },
      ];
  }
}
