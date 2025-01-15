import { useSetAtom } from "jotai";
import { Opphav, GjennomforingDto } from "@mr/api-client";
import { useNavigate } from "react-router";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { DupliserButton } from "../detaljside/DupliserButton";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  gjennomforing: GjennomforingDto;
}

export function DupliserGjennomforing({ gjennomforing }: Props) {
  const navigate = useNavigate();
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);

  function apneRedigeringForDupliseringAvTiltak() {
    setGjennomforingDetaljerTab("detaljer");
    navigate(`/avtaler/${gjennomforing.avtaleId}/gjennomforinger/skjema`, {
      state: {
        dupliserGjennomforing: {
          opphav: Opphav.MR_ADMIN_FLATE,
          avtaleId: gjennomforing.avtaleId,
          beskrivelse: gjennomforing.beskrivelse,
          faneinnhold: gjennomforing.faneinnhold,
        },
      },
    });
  }

  return (
    <HarSkrivetilgang ressurs="Gjennomføring">
      <DupliserButton
        title="Dupliser tiltaksgjennomføring"
        onClick={apneRedigeringForDupliseringAvTiltak}
      />
    </HarSkrivetilgang>
  );
}
