import {
  ApiError,
  TilsagnDto,
  TilsagnRequest,
  TiltaksgjennomforingDto,
  Tiltakskode,
} from "@mr/api-client";
import { SubmitHandler } from "react-hook-form";
import { Location, useLocation, useNavigate } from "react-router-dom";
import { SkjemaDetaljerContainer } from "../skjema/SkjemaDetaljerContainer";
import { SkjemaKolonne } from "../skjema/SkjemaKolonne";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { TilsagnSkjema } from "./TilsagnSkjema";
import { useOpprettTilsagn } from "./useOpprettTilsagn";
import { useTilsagnDefaults } from "@/api/tilsagn/useTilsagnDefaults";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  tilsagn?: TilsagnDto;
}

export function OpprettTilsagnContainer({ tiltaksgjennomforing, tilsagn }: Props) {
  const navigate = useNavigate();
  const location = useLocation() as Location<{ ekstratilsagn?: boolean }>;

  const erEkstratilsagn = location.state?.ekstratilsagn ?? false;

  const mutation = useOpprettTilsagn();
  const postData: SubmitHandler<InferredOpprettTilsagnSchema> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      id: data.id || window.crypto.randomUUID(),
      periodeStart: data.periodeStart,
      periodeSlutt: data.periodeSlutt,
      kostnadssted: data.kostnadssted,
      beregning: data.beregning,
      tiltaksgjennomforingId: tiltaksgjennomforing.id,
    };

    mutation.mutate(request, {
      onSuccess: navigerTilTilsagn,
    });
  };

  function navigerTilTilsagn() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/tilsagn`);
  }

  const skjemaProps = {
    gjennomforing: tiltaksgjennomforing,
    onSubmit: postData,
    onAvbryt: navigerTilTilsagn,
    isPending: mutation.isPending,
    error: mutation.error,
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
  onSubmit: (data: InferredOpprettTilsagnSchema) => void;
  onAvbryt: () => void;
  isPending: boolean;
  error: null | ApiError;
  prismodell: "AFT" | "FRI";
}

function OpprettTilsagnSkjema(props: TilsagnSkjemaProps) {
  const defaults = useTilsagnDefaults(props.gjennomforing.id);
  const kostnadssted = props.gjennomforing.navRegion?.enhetsnummer
    ? [props.gjennomforing.navRegion.enhetsnummer]
    : [];
  return (
    <TilsagnSkjema defaultValues={defaults.data} defaultKostnadssteder={kostnadssted} {...props} />
  );
}

function OpprettEkstratilsagnSkjema(props: TilsagnSkjemaProps) {
  return <TilsagnSkjema defaultValues={{}} defaultKostnadssteder={[]} {...props} />;
}

interface RedigerTilsagnSkjemaProps extends TilsagnSkjemaProps {
  tilsagn: TilsagnDto;
}

function RedigerTilsagnSkjema(props: RedigerTilsagnSkjemaProps) {
  const defaults = {
    id: props.tilsagn.id,
    beregning: props.tilsagn.beregning,
    kostnadssted: props.tilsagn.kostnadssted.enhetsnummer,
    periodeStart: props.tilsagn.periodeStart,
    periodeSlutt: props.tilsagn.periodeSlutt,
  };

  return <TilsagnSkjema defaultValues={defaults} defaultKostnadssteder={[]} {...props} />;
}
