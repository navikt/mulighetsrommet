import { ArrangorflateTilsagnDto, LabeledDataElement, LabeledDataElementType } from "api-client";
import { Definisjonsliste2 } from "../common/Definisjonsliste";
import { tekster } from "~/tekster";
import { tilsagnStatusElement } from "./TilsagnStatusTag";

interface Props {
  tilsagn: ArrangorflateTilsagnDto;
  minimal?: boolean;
  headingLevel?: "3" | "4";
}

export function TilsagnDetaljer({ tilsagn, headingLevel, minimal = false }: Props) {
  const tilsagnDetaljer: LabeledDataElement[] = !minimal
    ? [
        {
          label: "Status",
          type: LabeledDataElementType.INLINE,
          value: tilsagnStatusElement(tilsagn.status),
        },
        {
          label: "Tiltakstype",
          type: LabeledDataElementType.INLINE,
          value: { value: tilsagn.tiltakstype.navn, format: null },
        },
        {
          label: "Tiltaksnavn",
          type: LabeledDataElementType.INLINE,
          value: { value: tilsagn.gjennomforing.navn, format: null },
        },
      ]
    : [];

  return (
    <Definisjonsliste2
      headingLevel={headingLevel ?? "3"}
      className="p-4 border-1 border-border-divider rounded-lg w-xl"
      title={`${tekster.bokmal.tilsagn.tilsagntype(tilsagn.type)} ${tilsagn.bestillingsnummer}`}
      definitions={[...tilsagnDetaljer, ...tilsagn.beregning.entries]}
    />
  );
}
