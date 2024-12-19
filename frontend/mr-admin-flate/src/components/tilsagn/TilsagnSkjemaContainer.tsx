import { TilsagnType, TiltaksgjennomforingDto, Tiltakskode } from "@mr/api-client";
import { useNavigate } from "react-router-dom";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { DeepPartial } from "react-hook-form";
import { AftTilsagnSkjema } from "@/components/tilsagn/prismodell/aft/AftTilsagnSkjema";
import { FriTilsagnSkjema } from "@/components/tilsagn/prismodell/fri/FriTilsagnSkjema";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  defaults: DeepPartial<InferredTilsagn>;
}

export function TilsagnSkjemaContainer({ tiltaksgjennomforing, defaults }: Props) {
  const navigate = useNavigate();

  function navigerTilTilsagn() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/tilsagn`);
  }

  const kostnadssted = getKostnadssted(tiltaksgjennomforing, defaults.type);

  const props = {
    defaultKostnadssteder: kostnadssted,
    gjennomforing: tiltaksgjennomforing,
    onSuccess: navigerTilTilsagn,
    onAvbryt: navigerTilTilsagn,
  };

  switch (props.gjennomforing.tiltakstype.tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
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
