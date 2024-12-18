import { TilsagnDto, TilsagnType, TiltaksgjennomforingDto, Tiltakskode } from "@mr/api-client";
import { Location, useLocation, useNavigate } from "react-router-dom";
import { SkjemaDetaljerContainer } from "../skjema/SkjemaDetaljerContainer";
import { SkjemaKolonne } from "../skjema/SkjemaKolonne";
import { InferredTilsagnSchemaAft } from "./OpprettTilsagnSchema";
import { TilsagnSkjemaAft } from "./TilsagnSkjemaAft";
import { useTilsagnDefaults } from "@/api/tilsagn/useTilsagnDefaults";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  tilsagn?: TilsagnDto;
}

export function OpprettTilsagnContainer({ tiltaksgjennomforing, tilsagn }: Props) {
  const navigate = useNavigate();
  const location = useLocation() as Location<{ ekstratilsagn?: boolean }>;

  const erEkstratilsagn = location.state?.ekstratilsagn ?? false;

  function navigerTilTilsagn() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/tilsagn`);
  }

  const skjemaProps = {
    gjennomforing: tiltaksgjennomforing,
    onSuccess: navigerTilTilsagn,
    onAvbryt: navigerTilTilsagn,
    prismodell: prismodell(tiltaksgjennomforing),
  };

  return (
    <SkjemaDetaljerContainer>
      <SkjemaKolonne>
        {tilsagn ? (
          <RedigerTilsagnSkjema tilsagn={tilsagn} {...skjemaProps} />
        ) : erEkstratilsagn ? (
          <OpprettEkstratilsagnSkjema {...skjemaProps} />
        ) : (
          <OpprettTilsagnSkjema {...skjemaProps} />
        )}
      </SkjemaKolonne>
    </SkjemaDetaljerContainer>
  );
}

function prismodell(tiltaksgjennomforing: TiltaksgjennomforingDto): "AFT" | "FRI" {
  return tiltaksgjennomforing.tiltakstype.tiltakskode === Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
    ? "AFT"
    : "FRI";
}

interface TilsagnSkjemaProps {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  prismodell: "AFT" | "FRI";
}

function OpprettTilsagnSkjema(props: TilsagnSkjemaProps) {
  const defaults = useTilsagnDefaults(props.gjennomforing.id);
  const kostnadssted = props.gjennomforing.navRegion?.enhetsnummer
    ? [props.gjennomforing.navRegion.enhetsnummer]
    : [];
  return (
    <TilsagnSkjemaAft
      defaultValues={{ ...defaults.data, type: TilsagnType.TILSAGN }}
      defaultKostnadssteder={kostnadssted}
      {...props}
    />
  );
}

function OpprettEkstratilsagnSkjema(props: TilsagnSkjemaProps) {
  return (
    <TilsagnSkjemaAft
      defaultValues={{ type: TilsagnType.EKSTRATILSAGN }}
      defaultKostnadssteder={[]}
      {...props}
    />
  );
}

interface RedigerTilsagnSkjemaProps extends TilsagnSkjemaProps {
  tilsagn: TilsagnDto;
}

function RedigerTilsagnSkjema({ tilsagn, ...props }: RedigerTilsagnSkjemaProps) {
  if (tilsagn.beregning.type === "FRI") {
    throw new Error("FRI ikke støttet enda");
  }

  const { input, output } = tilsagn.beregning;

  const defaults: InferredTilsagnSchemaAft = {
    id: tilsagn.id,
    type: tilsagn.type,
    periodeStart: tilsagn.periodeStart,
    periodeSlutt: tilsagn.periodeSlutt,
    kostnadssted: tilsagn.kostnadssted.enhetsnummer,
    sats: output.sats,
    antallPlasser: input.antallPlasser,
  };

  return <TilsagnSkjemaAft defaultValues={defaults} defaultKostnadssteder={[]} {...props} />;
}
