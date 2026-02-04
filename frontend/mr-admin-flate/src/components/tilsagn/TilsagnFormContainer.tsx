import { TilsagnFormPrisPerManedsverk } from "@/components/tilsagn/form/TilsagnFormPrisPerManedsverk";
import { TilsagnFormFri } from "@/components/tilsagn/form/TilsagnFormFri";
import {
  GjennomforingDto,
  PrismodellDto,
  TilsagnBeregningType,
  TilsagnRequest,
} from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { TilsagnFormFastSatsPerTiltaksplassPerManed } from "./form/TilsagnFormFastSatsPerTiltaksplassPerManed";
import { TilsagnFormPrisPerTimeOppfolging } from "@/components/tilsagn/form/TilsagnFormPrisPerTimeOppfolging";
import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
  kostnadssteder: KostnadsstedOption[];
  defaults: TilsagnRequest;
}

export function TilsagnFormContainer({
  gjennomforing,
  prismodell,
  kostnadssteder,
  defaults,
}: Props) {
  const navigate = useNavigate();

  function navigerTilTilsagn() {
    navigate(`/gjennomforinger/${gjennomforing.id}/tilsagn`);
  }

  const props = {
    kostnadssteder,
    gjennomforing,
    prismodell,
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
