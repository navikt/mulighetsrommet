import { Alert, BodyLong, Button, Heading, ReadMore } from "@navikt/ds-react";
import { ApiError, Avtalestatus } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import { useAvtale } from "../../api/avtaler/useAvtale";
import styles from "./AvbrytAvtale.module.scss";
import AvbrytAvtaleModal from "../modal/AvbrytAvtaleModal";

interface Props {
  handleAvbrytAvtale: () => void;
}

export function AvbrytAvtale({ handleAvbrytAvtale }: Props) {
  const { data: avtale } = useAvtale();
  const mutation = useAvbrytAvtale();
  const [error, setError] = useState("");
  const [avbrytModal, setAvbrytModal] = useState(false);

  const erAktivAvtale = avtale?.avtalestatus === Avtalestatus.AKTIV;

  const avbrytAvtale = () => {
    if (!avtale?.id) throw new Error("Fant ingen avtaleId");
    else setAvbrytModal(true);
  };

  useEffect(() => {
    if (mutation.isSuccess) {
      handleAvbrytAvtale();
    }

    if (mutation.isError) {
      const error = mutation.error as ApiError;
      setError(error.body);
    }
  }, [mutation]);

  if (avtale?.avtalestatus === Avtalestatus.AVSLUTTET) {
    // Trenger ikke avbryte en avtale som allerede er avsluttet
    return null;
  }

  return (
    <>
      <div className={styles.warning_container}>
        {erAktivAvtale ? (
          <ReadMore header="Hva betyr det å avbryte avtalen?">
            <BodyLong>
              Hvis avtalens startdato er passert kan du avbryte avtalen. Den vil
              da bli satt som avbrutt i systemet. Du kan ikke avbryte en avtale
              som har tiltaksgjennomføringer tilknyttet seg.
            </BodyLong>
          </ReadMore>
        ) : null}

        <Button
          type="button"
          size="small"
          variant="danger"
          onClick={avbrytAvtale}
        >
          {erAktivAvtale ? "Avbryt avtalen" : "Slett avtalen"}
        </Button>

        {error ? (
          <Alert variant="warning">
            <Heading spacing size="small" level="3">
              Klarte ikke avbryte avtale
            </Heading>
            {error}
          </Alert>
        ) : null}
      </div>

      <AvbrytAvtaleModal
        modalOpen={avbrytModal}
        handleClose={() => setAvbrytModal(false)}
        avtale={avtale}
        erAktivAvtale={avtale?.avtalestatus === Avtalestatus.AKTIV}
      />
    </>
  );
}
