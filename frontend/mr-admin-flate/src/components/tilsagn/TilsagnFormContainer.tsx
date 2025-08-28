import { TilsagnFormPrisPerManedsverk } from "@/components/tilsagn/form/TilsagnFormPrisPerManedsverk";
import { TilsagnFormFri } from "@/components/tilsagn/form/TilsagnFormFri";
import {
  AvtaleDto,
  GjennomforingDto,
  PrismodellDto,
  TilsagnBeregningType,
  TilsagnRequest,
  TilsagnType,
} from "@mr/api-client-v2";
import { useNavigate } from "react-router";
import { TilsagnFormFastSatsPerTiltaksplassPerManed } from "./form/TilsagnFormFastSatsPerTiltaksplassPerManed";
import { TilsagnFormPrisPerTimeOppfolging } from "@/components/tilsagn/form/TilsagnFormPrisPerTimeOppfolging";

interface Props {
  avtale: AvtaleDto;
  gjennomforing: GjennomforingDto;
  defaults: TilsagnRequest;
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

  const beregning = getTilsagnBeregningType(avtale.prismodell);

  switch (beregning) {
    case TilsagnBeregningType.PRIS_PER_UKESVERK:
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

function getTilsagnBeregningType(prismodell: PrismodellDto): TilsagnBeregningType {
  switch (prismodell.type) {
    case "ANNEN_AVTALT_PRIS":
      return TilsagnBeregningType.FRI;
    case "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK":
      return TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED;
    case "AVTALT_PRIS_PER_MANEDSVERK":
      return TilsagnBeregningType.PRIS_PER_MANEDSVERK;
    case "AVTALT_PRIS_PER_UKESVERK":
      return TilsagnBeregningType.PRIS_PER_UKESVERK;
    case "AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER":
      return TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING;
  }
}
