import { LayersPlusIcon } from "@navikt/aksel-icons";
import styles from "./DupliserTiltak.module.scss";
import { Button } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function DupliserTiltak({ tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();

  function apneRedigeringForDupliseringAvTiltak() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/skjema`, {
      state: {
        tiltaksgjennomforing: {
          ...tiltaksgjennomforing,
          id: window.crypto.randomUUID(),
          tiltaksnummer: "",
          startDato: undefined,
          sluttDato: undefined,
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
      />
    </Button>
  );
}
