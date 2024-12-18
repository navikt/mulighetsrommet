import { TilsagnDto, TilsagnType, TiltaksgjennomforingDto } from "@mr/api-client";
import { AftTilsagnSkjema } from "@/components/tilsagn/prismodell/aft/AftTilsagnSkjema";
import { FriTilsagnSkjema } from "@/components/tilsagn/prismodell/fri/FriTilsagnSkjema";

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  tilsagn: TilsagnDto;
  onSuccess: () => void;
  onAvbryt: () => void;
}

export function RedigerTilsagn({ tilsagn, ...props }: Props) {
  const tilsagnDefaults = {
    id: tilsagn.id,
    type: tilsagn.type,
    periodeStart: tilsagn.periodeStart,
    periodeSlutt: tilsagn.periodeSlutt,
    kostnadssted: tilsagn.kostnadssted.enhetsnummer,
  };

  const kostnadssted = getKostnadssted(tilsagn, props.gjennomforing);

  switch (tilsagn.beregning.type) {
    case "AFT":
      return (
        <AftTilsagnSkjema
          defaultValues={{
            ...tilsagnDefaults,
            sats: tilsagn.beregning.output.sats,
            antallPlasser: tilsagn.beregning.input.antallPlasser,
          }}
          defaultKostnadssteder={kostnadssted}
          {...props}
        />
      );

    case "FRI":
      return (
        <FriTilsagnSkjema
          defaultValues={{
            ...tilsagnDefaults,
            belop: tilsagn.beregning.output.belop,
          }}
          defaultKostnadssteder={kostnadssted}
          {...props}
        />
      );
  }
}

function getKostnadssted(tilsagn: TilsagnDto, gjennomforing: TiltaksgjennomforingDto) {
  return tilsagn.type === TilsagnType.TILSAGN && gjennomforing.navRegion?.enhetsnummer
    ? [gjennomforing.navRegion.enhetsnummer]
    : [];
}
