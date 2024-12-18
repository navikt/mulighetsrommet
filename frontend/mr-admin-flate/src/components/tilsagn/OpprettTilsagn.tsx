import { TilsagnType, TiltaksgjennomforingDto, Tiltakskode } from "@mr/api-client";
import { useTilsagnDefaults } from "@/api/tilsagn/useTilsagnDefaults";
import { AftTilsagnSkjema } from "@/components/tilsagn/prismodell/aft/AftTilsagnSkjema";
import { FriTilsagnSkjema } from "@/components/tilsagn/prismodell/fri/FriTilsagnSkjema";

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
}

export function OpprettTilsagn(props: Props) {
  const defaults = useTilsagnDefaults(props.gjennomforing.id);

  const kostnadssted = props.gjennomforing.navRegion?.enhetsnummer
    ? [props.gjennomforing.navRegion.enhetsnummer]
    : [];

  const defaultValues = { ...defaults.data, type: TilsagnType.TILSAGN };

  switch (props.gjennomforing.tiltakstype.tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
      return (
        <AftTilsagnSkjema
          defaultValues={defaultValues}
          defaultKostnadssteder={kostnadssted}
          {...props}
        />
      );

    default:
      return (
        <FriTilsagnSkjema
          defaultValues={defaultValues}
          defaultKostnadssteder={kostnadssted}
          {...props}
        />
      );
  }
}
