import { useSetAtom } from "jotai";
import { Opphav, TiltaksgjennomforingDto } from "@mr/api-client";
import { useNavigate } from "react-router-dom";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { DupliserButton } from "../detaljside/DupliserButton";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
}

export function DupliserTiltak({ tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);

  function apneRedigeringForDupliseringAvTiltak() {
    setGjennomforingDetaljerTab("detaljer");
    navigate(`/avtaler/${tiltaksgjennomforing.avtaleId}/tiltaksgjennomforinger/skjema`, {
      state: {
        dupliserTiltaksgjennomforing: {
          opphav: Opphav.MR_ADMIN_FLATE,
          avtaleId: tiltaksgjennomforing.avtaleId,
          beskrivelse: tiltaksgjennomforing.beskrivelse,
          faneinnhold: tiltaksgjennomforing.faneinnhold,
        },
      },
    });
  }

  return (
    <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
      <DupliserButton
        title="Dupliser tiltaksgjennomføring"
        onClick={apneRedigeringForDupliseringAvTiltak}
      />
    </HarSkrivetilgang>
  );
}
