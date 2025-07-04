import { TilsagnFormPrisPerManedsverk } from "@/components/tilsagn/prismodell/TilsagnFormPrisPerManedsverk";
import { TilsagnFormFri } from "@/components/tilsagn/prismodell/TilsagnFormFri";
import {
  AvtaleDto,
  GjennomforingDto,
  Prismodell,
  TilsagnBeregningType,
  TilsagnType,
} from "@mr/api-client-v2";
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
    case TilsagnBeregningType.PRIS_PER_MANEDSVERK:
      return (
        <TilsagnFormPrisPerManedsverk
          defaultValues={{
            ...defaults,
            beregning: {
              ...defaults.beregning,
              type: TilsagnBeregningType.PRIS_PER_MANEDSVERK,
            },
          }}
          {...props}
        />
      );
    case TilsagnBeregningType.FRI:
      return (
        <TilsagnFormFri
          defaultValues={{
            ...defaults,
            beregning: {
              ...defaults.beregning,
              prisbetingelser: avtale.prisbetingelser,
              type: TilsagnBeregningType.FRI,
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
): TilsagnBeregningType | undefined {
  switch (prismodell) {
    case Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
    case Prismodell.AVTALT_PRIS_PER_MANEDSVERK:
      return TilsagnBeregningType.PRIS_PER_MANEDSVERK;
    case Prismodell.ANNEN_AVTALT_PRIS:
      return TilsagnBeregningType.FRI;
    default:
      return undefined;
  }
}
