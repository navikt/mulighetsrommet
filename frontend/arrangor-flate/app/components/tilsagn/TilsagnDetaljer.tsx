import { ArrangorflateTilsagnDto } from "api-client";
import { Definisjonsliste, Definition } from "../common/Definisjonsliste";
import { tekster } from "~/tekster";
import { TilsagnStatusTag } from "./TilsagnStatusTag";

interface Props {
  tilsagn: ArrangorflateTilsagnDto;
  minimal?: boolean;
  headingLevel?: "3" | "4";
}

export function TilsagnDetaljer({ tilsagn, headingLevel, minimal = false }: Props) {
  const tilsagnDetaljer: Definition[] = !minimal
    ? [
        { key: "Status", value: <TilsagnStatusTag status={tilsagn.status} /> },
        { key: "Tiltakstype", value: tilsagn.tiltakstype.navn },
        { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
      ]
    : [];

  return (
    <Definisjonsliste
      headingLevel={headingLevel ?? "3"}
      className="p-4 border-1 border-border-divider rounded-lg w-xl"
      title={`${tekster.bokmal.tilsagn.tilsagntype(tilsagn.type)} ${tilsagn.bestillingsnummer}`}
      definitions={[...tilsagnDetaljer, ...tilsagn.beregning.entries]}
    />
  );
}
