import { TilsagnSkjemaForhandsgodkjent } from "@/components/tilsagn/prismodell/TilsagnSkjemaForhandsgodkjent";
import { TilsagnSkjemaFri } from "@/components/tilsagn/prismodell/TilsagnSkjemaFri";
import { AvtaleDto, Avtaletype, TilsagnType, GjennomforingDto } from "@mr/api-client";
import { useNavigate } from "react-router";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { DeepPartial } from "react-hook-form";

interface Props {
  avtale: AvtaleDto;
  gjennomforing: GjennomforingDto;
  defaults: DeepPartial<InferredTilsagn>;
}

export function TilsagnSkjemaContainer({ avtale, gjennomforing, defaults }: Props) {
  const navigate = useNavigate();

  function navigerTilTilsagn() {
    navigate(`/gjennomforinger/${gjennomforing.id}/tilsagn`);
  }

  const kostnadssted = getKostnadssted(gjennomforing, defaults.type);

  const props = {
    defaultKostnadssteder: kostnadssted,
    gjennomforing: gjennomforing,
    onSuccess: navigerTilTilsagn,
    onAvbryt: navigerTilTilsagn,
  };

  switch (avtale.avtaletype) {
    case Avtaletype.FORHAANDSGODKJENT:
      return (
        <TilsagnSkjemaForhandsgodkjent
          defaultValues={{
            ...defaults,
            beregning: { ...defaults.beregning, type: "FORHANDSGODKJENT" },
          }}
          {...props}
        />
      );

    default:
      return (
        <TilsagnSkjemaFri
          defaultValues={{
            ...defaults,
            beregning: { ...defaults.beregning, type: "FRI" },
          }}
          {...props}
        />
      );
  }
}

function getKostnadssted(gjennomforing: GjennomforingDto, type?: TilsagnType) {
  return type === TilsagnType.TILSAGN && gjennomforing.navRegion?.enhetsnummer
    ? [gjennomforing.navRegion.enhetsnummer]
    : [];
}
