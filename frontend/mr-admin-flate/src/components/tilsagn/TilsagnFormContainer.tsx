import { TilsagnFormForhandsgodkjent } from "@/components/tilsagn/prismodell/TilsagnFormForhandsgodkjent";
import { TilsagnFormFri } from "@/components/tilsagn/prismodell/TilsagnFormFri";
import { AvtaleDto, GjennomforingDto, Prismodell, TilsagnType } from "@mr/api-client-v2";
import { useNavigate } from "react-router";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { DeepPartial } from "react-hook-form";
import { Alert } from "@navikt/ds-react";

interface Props {
  avtale: AvtaleDto;
  gjennomforing: GjennomforingDto;
  defaults: DeepPartial<InferredTilsagn>;
}

export function TilsagnFormContainer({ avtale, gjennomforing, defaults }: Props) {
  const navigate = useNavigate();

  function navigerTilTilsagn() {
    navigate(`/gjennomforinger/${gjennomforing.id}/tilsagn`);
  }

  const regionerForKostnadssteder = getRegionerForKostnadssteder(gjennomforing, defaults.type);

  const props = {
    regioner: regionerForKostnadssteder,
    gjennomforing: gjennomforing,
    onSuccess: navigerTilTilsagn,
    onAvbryt: navigerTilTilsagn,
  };

  const beregning = defaults.beregning?.type ?? getTilsagnBeregningType(avtale.prismodell);
  switch (beregning) {
    case "AVTALT_PRIS_PER_MANEDSVERK":
      return (
        <TilsagnFormForhandsgodkjent
          defaultValues={{
            ...defaults,
            beregning: {
              ...defaults.beregning,
              type: "AVTALT_PRIS_PER_MANEDSVERK",
            },
          }}
          {...props}
        />
      );
    case "FRI":
      return (
        <TilsagnFormFri
          defaultValues={{
            ...defaults,
            beregning: {
              ...defaults.beregning,
              prisbetingelser: avtale.prisbetingelser,
              type: "FRI",
            },
          }}
          {...props}
        />
      );
    default:
      return <Alert variant={"warning"}>Prismodell mangler</Alert>;
  }
}

function getRegionerForKostnadssteder(gjennomforing: GjennomforingDto, type?: TilsagnType) {
  return type === TilsagnType.TILSAGN
    ? gjennomforing.kontorstruktur.map((struktur) => struktur.region.enhetsnummer)
    : [];
}

function getTilsagnBeregningType(
  prismodell: Prismodell | undefined | null,
): "FRI" | "AVTALT_PRIS_PER_MANEDSVERK" | undefined {
  switch (prismodell) {
    case Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
    case Prismodell.AVTALT_PRIS_PER_MANEDSVERK:
      return "AVTALT_PRIS_PER_MANEDSVERK";
    case Prismodell.ANNEN_AVTALT_PRIS:
      return "FRI";
    default:
      return undefined;
  }
}
