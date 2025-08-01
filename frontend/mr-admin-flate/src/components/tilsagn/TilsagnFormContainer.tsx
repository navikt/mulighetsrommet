import { TilsagnFormPrisPerManedsverk } from "@/components/tilsagn/form/TilsagnFormPrisPerManedsverk";
import { TilsagnFormFri } from "@/components/tilsagn/form/TilsagnFormFri";
import {
  AvtaleDto,
  GjennomforingDto,
  Prismodell,
  TilsagnBeregningType,
  TilsagnType,
} from "@mr/api-client-v2";
import { useNavigate } from "react-router";
import { InferredTilsagn } from "@/components/tilsagn/form/TilsagnSchema";
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

  const beregning =
    (defaults.beregning?.type ?? avtale.prismodell)
      ? getTilsagnBeregningType(avtale.prismodell)
      : null;
  switch (beregning) {
    case TilsagnBeregningType.PRIS_PER_UKESVERK:
    case TilsagnBeregningType.PRIS_PER_MANEDSVERK:
      return (
        <TilsagnFormPrisPerManedsverk
          defaultValues={{
            ...defaults,
            beregning: { ...defaults.beregning, type: beregning },
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
              type: beregning,
            },
          }}
          {...props}
        />
      );
    case null:
      return <Alert variant={"warning"}>Prismodell mangler</Alert>;
  }
}

function getRegionerForKostnadssteder(gjennomforing: GjennomforingDto, type?: TilsagnType) {
  return type === TilsagnType.TILSAGN
    ? gjennomforing.kontorstruktur.map((struktur) => struktur.region.enhetsnummer)
    : [];
}

function getTilsagnBeregningType(prismodell: Prismodell): TilsagnBeregningType {
  switch (prismodell) {
    case Prismodell.ANNEN_AVTALT_PRIS:
      return TilsagnBeregningType.FRI;
    case Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
    case Prismodell.AVTALT_PRIS_PER_MANEDSVERK:
      return TilsagnBeregningType.PRIS_PER_MANEDSVERK;
    case Prismodell.AVTALT_PRIS_PER_UKESVERK:
      return TilsagnBeregningType.PRIS_PER_UKESVERK;
  }
}
