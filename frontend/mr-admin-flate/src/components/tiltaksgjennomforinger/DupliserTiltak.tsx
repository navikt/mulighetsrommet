import { LayersPlusIcon } from "@navikt/aksel-icons";
import styles from "./DupliserTiltak.module.scss";
import { Button } from "@navikt/ds-react";
import { Opphav, Tiltaksgjennomforing, Toggles } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";
import { useFeatureToggle } from "../../api/features/feature-toggles";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function DupliserTiltak({ tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const { data: kanDuplisereTiltak } = useFeatureToggle(
    Toggles.MR_ADMIN_FLATE_KAN_DUPLISERE_TILTAK,
  );

  if (!kanDuplisereTiltak) return null;

  function apneRedigeringForDupliseringAvTiltak() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/skjema`, {
      state: {
        tiltaksgjennomforing: {
          ...tiltaksgjennomforing,
          id: window.crypto.randomUUID(),
          tiltaksnummer: "",
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
