import { formaterDato } from "~/utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { ArrangorflateTilsagn } from "api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { formaterTilsagnType } from "../tilsagn/TilsagnTable";
import { gjenstaendeBelop } from "~/utils/tilsagn";

interface Props {
  tilsagn: ArrangorflateTilsagn;
  utenTittel?: boolean;
}

export default function UtbetalingTilsagnDetaljer({ tilsagn, utenTittel }: Props) {
  return (
    <Definisjonsliste
      title={utenTittel ? "" : "Tilsagnsdetaljer"}
      headingLevel="3"
      definitions={[
        {
          key: "Tilsagnsnummer",
          value: tilsagn.bestillingsnummer,
        },
        {
          key: "Type",
          value: formaterTilsagnType(tilsagn.type),
        },
        { key: "Periode start", value: formaterDato(tilsagn.periode.start) },
        { key: "Periode slutt", value: formaterDato(tilsagn.periode.slutt) },
        { key: "Beløp", value: formaterNOK(tilsagn.beregning.output.belop) },
        {
          key: "Gjenstående",
          value: formaterNOK(gjenstaendeBelop(tilsagn)),
        },
      ]}
    />
  );
}
