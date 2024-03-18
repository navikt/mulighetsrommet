import { Opphav, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";
import { useMigrerteTiltakstyper } from "../../api/tiltakstyper/useMigrerteTiltakstyper";
import { DupliserButton } from "../detaljside/DupliserButton";
import { useAtom } from "jotai";
import { gjennomforingDetaljerTabAtom } from "../../api/atoms";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function DupliserTiltak({ tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_, setGjennomforingDetaljerTab] = useAtom(gjennomforingDetaljerTabAtom);

  if (!migrerteTiltakstyper?.includes(tiltaksgjennomforing.tiltakstype.arenaKode)) return null;

  function apneRedigeringForDupliseringAvTiltak() {
    setGjennomforingDetaljerTab("detaljer");
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/skjema`, {
      state: {
        tiltaksgjennomforing: {
          ...tiltaksgjennomforing,
          id: window.crypto.randomUUID(),
          startDato: undefined,
          sluttDato: undefined,
          opphav: Opphav.MR_ADMIN_FLATE,
        },
      },
    });
  }

  return (
    <DupliserButton
      title="Dupliser tiltaksgjennomføring"
      onClick={apneRedigeringForDupliseringAvTiltak}
    />
  );
}
