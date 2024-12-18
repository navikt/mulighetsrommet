import { TilsagnType, TiltaksgjennomforingDto, Tiltakskode } from "@mr/api-client";
import { AftTilsagnSkjema } from "@/components/tilsagn/prismodell/aft/AftTilsagnSkjema";
import { FriTilsagnSkjema } from "@/components/tilsagn/prismodell/fri/FriTilsagnSkjema";

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
}

export function OpprettEkstratilsagn(props: Props) {
  const defaultValues = { type: TilsagnType.EKSTRATILSAGN };

  switch (props.gjennomforing.tiltakstype.tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
      return (
        <AftTilsagnSkjema defaultValues={defaultValues} defaultKostnadssteder={[]} {...props} />
      );

    default:
      return (
        <FriTilsagnSkjema defaultValues={defaultValues} defaultKostnadssteder={[]} {...props} />
      );
  }
}
