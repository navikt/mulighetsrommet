import { TilsagnDto, TilsagnRequest, TiltaksgjennomforingDto, Tiltakskode } from "@mr/api-client";
import { SubmitHandler } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { SkjemaDetaljerContainer } from "../skjema/SkjemaDetaljerContainer";
import { SkjemaKolonne } from "../skjema/SkjemaKolonne";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { TilsagnSkjema } from "./TilsagnSkjema";
import { useOpprettTilsagn } from "./useOpprettTilsagn";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  tilsagn?: TilsagnDto;
  tilsagnSkalGodkjennes: boolean;
}

export function OpprettTilsagnContainer({ tiltaksgjennomforing, tilsagn }: Props) {
  const navigate = useNavigate();
  const mutation = useOpprettTilsagn();

  const postData: SubmitHandler<InferredOpprettTilsagnSchema> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      id: data.id || window.crypto.randomUUID(),
      periodeStart: data.periode.start,
      periodeSlutt: data.periode.slutt,
      kostnadssted: data.kostnadssted,
      beregning: data.beregning,
      tiltaksgjennomforingId: tiltaksgjennomforing.id,
    };

    mutation.mutate(request, {
      onSuccess: navigerTilGjennomforing,
    });
  };

  function navigerTilGjennomforing() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`);
  }

  function prismodell(tiltaksgjennomforing: TiltaksgjennomforingDto): "AFT" | "FRI" {
    return tiltaksgjennomforing.tiltakstype.tiltakskode === Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
      ? "AFT"
      : "FRI";
  }

  return (
    <SkjemaDetaljerContainer>
      <SkjemaKolonne>
        <TilsagnSkjema
          tiltaksgjennomforing={tiltaksgjennomforing}
          tilsagn={tilsagn}
          onSubmit={postData}
          mutation={mutation}
          onAvbryt={navigerTilGjennomforing}
          prismodell={prismodell(tiltaksgjennomforing)}
        />
      </SkjemaKolonne>
    </SkjemaDetaljerContainer>
  );
}
