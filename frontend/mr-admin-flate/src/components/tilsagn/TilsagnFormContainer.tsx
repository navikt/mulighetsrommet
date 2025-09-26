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

interface Props {
  gjennomforing: GjennomforingDto;
  defaults: TilsagnRequest;
}

export function TilsagnFormContainer({ gjennomforing, defaults }: Props) {
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

function getRegionerForKostnadssteder(gjennomforing: GjennomforingDto, type?: TilsagnType) {
  return type === TilsagnType.TILSAGN
    ? gjennomforing.kontorstruktur.map((struktur) => struktur.region.enhetsnummer)
    : [];
}
