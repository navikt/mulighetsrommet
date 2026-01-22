import { TilsagnFormPrisPerManedsverk } from "@/components/tilsagn/form/TilsagnFormPrisPerManedsverk";
import { TilsagnFormFri } from "@/components/tilsagn/form/TilsagnFormFri";
import {
  GjennomforingDto,
  TilsagnBeregningType,
  TilsagnRequest,
  TilsagnType,
} from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { TilsagnFormFastSatsPerTiltaksplassPerManed } from "./form/TilsagnFormFastSatsPerTiltaksplassPerManed";
import { TilsagnFormPrisPerTimeOppfolging } from "@/components/tilsagn/form/TilsagnFormPrisPerTimeOppfolging";
import { useKostnadsstedFilter } from "@/api/enhet/useKostnadsstedFilter";
import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";

interface Props {
  gjennomforing: GjennomforingDto;
  defaults: TilsagnRequest;
}

export function TilsagnFormContainer({ gjennomforing, defaults }: Props) {
  const navigate = useNavigate();

  function navigerTilTilsagn() {
    navigate(`/gjennomforinger/${gjennomforing.id}/tilsagn`);
  }

  const kostnadssteder = useRelevanteKostnadsstederForTilsagn(gjennomforing, defaults.type);

  const props = {
    kostnadssteder,
    gjennomforing,
    onSuccess: navigerTilTilsagn,
    onAvbryt: navigerTilTilsagn,
  };

  const beregning = defaults.beregning.type;

  switch (beregning) {
    case TilsagnBeregningType.PRIS_PER_UKESVERK:
    case TilsagnBeregningType.PRIS_PER_HELE_UKESVERK:
    case TilsagnBeregningType.PRIS_PER_MANEDSVERK:
      return (
        <TilsagnFormPrisPerManedsverk
          defaultValues={{
            ...defaults,
            beregning: {
              ...defaults.beregning,
              type: beregning,
            },
          }}
          {...props}
        />
      );
    case TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED:
      return (
        <TilsagnFormFastSatsPerTiltaksplassPerManed
          defaultValues={{
            ...defaults,
            beregning: {
              ...defaults.beregning,
              type: beregning,
            },
          }}
          {...props}
        />
      );
    case TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING:
      return (
        <TilsagnFormPrisPerTimeOppfolging
          defaultValues={{
            ...defaults,
            beregning: {
              ...defaults.beregning,
              type: beregning,
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
              type: beregning,
            },
          }}
          {...props}
        />
      );
  }
}

function useRelevanteKostnadsstederForTilsagn(
  gjennomforing: GjennomforingDto,
  type?: TilsagnType,
): KostnadsstedOption[] {
  const { data: kostnadssteder } = useKostnadsstedFilter();

  const relevanteRegioner =
    type === TilsagnType.TILSAGN
      ? gjennomforing.kontorstruktur.map((struktur) => struktur.region.enhetsnummer)
      : kostnadssteder.map((region) => region.enhetsnummer);

  return kostnadssteder
    .filter((region) => relevanteRegioner.includes(region.enhetsnummer))
    .flatMap((region) => region.enheter);
}
