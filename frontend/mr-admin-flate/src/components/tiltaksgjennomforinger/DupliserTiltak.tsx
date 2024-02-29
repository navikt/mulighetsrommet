import { LayersPlusIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import { Opphav, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";
import { useMigrerteTiltakstyper } from "../../api/tiltakstyper/useMigrerteTiltakstyper";
import styles from "./DupliserTiltak.module.scss";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function DupliserTiltak({ tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();

  if (!migrerteTiltakstyper?.includes(tiltaksgjennomforing.tiltakstype.arenaKode)) return null;

  function apneRedigeringForDupliseringAvTiltak() {
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
    <Button
      title="Dupliser tiltaksgjennomføring"
      className={styles.button}
      onClick={apneRedigeringForDupliseringAvTiltak}
    >
      <LayersPlusIcon
        style={{ margin: "0 auto", display: "block" }}
        color="white"
        fontSize="1.5rem"
        aria-label="Ikon for duplisering av dokument"
      />
    </Button>
  );
}
