import { AvtaleDto, Avtaletype, TilsagnType, TiltaksgjennomforingDto } from "@mr/api-client";
import { useNavigate } from "react-router-dom";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { DeepPartial } from "react-hook-form";
import { AftTilsagnSkjema } from "@/components/tilsagn/prismodell/aft/AftTilsagnSkjema";
import { FriTilsagnSkjema } from "@/components/tilsagn/prismodell/fri/FriTilsagnSkjema";

interface Props {
  avtale: AvtaleDto;
  gjennomforing: TiltaksgjennomforingDto;
  defaults: DeepPartial<InferredTilsagn>;
}

export function TilsagnSkjemaContainer({ avtale, gjennomforing, defaults }: Props) {
  const navigate = useNavigate();

  function navigerTilTilsagn() {
    navigate(`/tiltaksgjennomforinger/${gjennomforing.id}/tilsagn`);
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
        <AftTilsagnSkjema
          defaultValues={{
            ...defaults,
            beregning: { ...defaults.beregning, type: "AFT" },
          }}
          {...props}
        />
      );

    default:
      return (
        <FriTilsagnSkjema
          defaultValues={{
            ...defaults,
            beregning: { ...defaults.beregning, type: "FRI" },
          }}
          {...props}
        />
      );
  }
}

function getKostnadssted(gjennomforing: TiltaksgjennomforingDto, type?: TilsagnType) {
  return type === TilsagnType.TILSAGN && gjennomforing.navRegion?.enhetsnummer
    ? [gjennomforing.navRegion.enhetsnummer]
    : [];
}
