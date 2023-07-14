import { Alert, BodyLong, Button, Heading, ReadMore } from "@navikt/ds-react";
import { ApiError, Avtalestatus } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import { useAvtale } from "../../api/avtaler/useAvtale";
import styles from "./AvbrytAvtale.module.scss";

interface Props {
  onAvbryt: () => void;
}

export function AvbrytAvtale({ onAvbryt }: Props) {
  const { data: avtale } = useAvtale();
  const mutation = useAvbrytAvtale();
  const [error, setError] = useState("");

  const avbrytAvtale = () => {
    if (!avtale?.id) throw new Error("Fant ingen avtaleId");

    const dialog = confirm("Er du sikker på at du vil avbryte avtalen?");
    if (dialog) {
      mutation.mutate(avtale.id);
    }
  };

  useEffect(() => {
    if (mutation.isSuccess) {
      onAvbryt();
    }

    if (mutation.isError) {
      const error = mutation.error as ApiError;
      setError(error.body);
    }
  }, [mutation]);

  console.log(avtale);
  if (avtale?.avtalestatus === Avtalestatus.AVSLUTTET) {
    // Trenger ikke avbryt en avtale som allerede er avsluttet
    return null;
  }

  return (
    <div className={styles.warning_container}>
      <ReadMore header="Hva betyr det å avbryte avtalen?">
        <BodyLong>
          Hvis avtalens startdato er passert kan du avbryte avtalen. Den vil da
          bli satt som avbrutt i systemet. Du kan ikke avbryte en avtale som har
          tiltaksgjennomføringer tilknyttet seg.
        </BodyLong>
      </ReadMore>

      <Button
        type="button"
        size="small"
        variant="danger"
        onClick={avbrytAvtale}
      >
        Jeg vil avbryte avtalen
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
  );
}
