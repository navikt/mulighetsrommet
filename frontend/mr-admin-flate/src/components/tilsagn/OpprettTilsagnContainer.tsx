import { TilsagnDto, TilsagnRequest, TiltaksgjennomforingDto, Tiltakskode } from "@mr/api-client";
import { SubmitHandler } from "react-hook-form";
import { useNavigate } from "react-router-dom";
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

  const defaults = useTilsagnDefaults(tiltaksgjennomforing.id);

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

  function prismodell(tiltaksgjennomforing: TiltaksgjennomforingDto): "AFT" | "FRI" {
    return tiltaksgjennomforing.tiltakstype.tiltakskode === Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
      ? "AFT"
      : "FRI";
  }

  return (
    <>
      <SkjemaDetaljerContainer>
        <SkjemaKolonne>
          <TilsagnSkjema
            tiltaksgjennomforing={tiltaksgjennomforing}
            defaults={defaults.data}
            tilsagn={tilsagn}
            onSubmit={postData}
            mutation={mutation}
            onAvbryt={navigerTilTilsagn}
            prismodell={prismodell(tiltaksgjennomforing)}
          />
        </SkjemaKolonne>
      </SkjemaDetaljerContainer>
    </>
  );
}
