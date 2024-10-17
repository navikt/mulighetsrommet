import { ArrangorflateTilsagn, TilsagnBeregningAFT } from "@mr/api-client";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterDato } from "~/utils";
import { formaterTall } from "@mr/frontend-common/utils/utils";

interface Props {
  tilsagn: ArrangorflateTilsagn;
}

export function AFTTilsagnDetaljer({ tilsagn }: Props) {
  const beregning = tilsagn.beregning as TilsagnBeregningAFT;

  return (
    <Definisjonsliste
      className="mt-4"
      definitions={[
        {
          key: "Tilsagnsperiode",
          value: `${formaterDato(tilsagn.periodeStart)} - ${formaterDato(tilsagn.periodeSlutt)}`,
        },
        { key: "Tiltakstype", value: tilsagn.tiltakstype.navn },
        { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
        { key: "Antall plasser", value: String(beregning.antallPlasser) },
        { key: "Sats", value: formaterTall(beregning.sats) },
        { key: "Beløp", value: formaterTall(beregning.belop) },
        { key: "Beløp", value: formaterTall(beregning.belop) },
      ]}
    />
  );
}
