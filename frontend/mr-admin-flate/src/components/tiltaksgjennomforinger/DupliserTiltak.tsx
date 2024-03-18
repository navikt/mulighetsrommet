import { useSetAtom } from "jotai";
import { Opphav, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";
import { gjennomforingDetaljerTabAtom } from "../../api/atoms";
import { useMigrerteTiltakstyper } from "../../api/tiltakstyper/useMigrerteTiltakstyper";
import { DupliserButton } from "../detaljside/DupliserButton";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function DupliserTiltak({ tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);

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
