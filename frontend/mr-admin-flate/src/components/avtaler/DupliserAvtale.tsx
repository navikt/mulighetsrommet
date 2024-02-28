import { LayersPlusIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import { Avtale, Opphav } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";
import styles from "./DupliserAvtale.module.scss";
import { useMigrerteTiltakstyperForAvtaler } from "../../api/tiltakstyper/useMigrerteTiltakstyper";

interface Props {
  avtale: Avtale;
}

export function DupliserAvtale({ avtale }: Props) {
  const navigate = useNavigate();
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyperForAvtaler();

  if (!migrerteTiltakstyper.includes(avtale.tiltakstype.arenaKode)) return null;

  function apneRedigeringForDupliseringAvAvtale() {
    navigate(`/avtaler/${avtale.id}/skjema`, {
      state: {
        avtale: {
          ...avtale,
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
      title="Dupliser avtale"
      className={styles.button}
      onClick={apneRedigeringForDupliseringAvAvtale}
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
