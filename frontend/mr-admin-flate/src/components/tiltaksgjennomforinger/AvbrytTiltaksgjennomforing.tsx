import { Alert, BodyLong, Button, Heading, ReadMore } from "@navikt/ds-react";
import { ApiError, TiltaksgjennomforingStatus } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { useAvbrytTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useAvbrytTiltaksgjennomforing";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import styles from "./AvbrytTiltaksgjennomforing.module.scss";

interface Props {
  onAvbryt: () => void;
}

export function AvbrytTiltaksgjennomforing({ onAvbryt }: Props) {
  const { data: gjennomforing } = useTiltaksgjennomforingById();
  const mutation = useAvbrytTiltaksgjennomforing();
  const [error, setError] = useState("");

  const avbrytAvtale = () => {
    if (!gjennomforing?.id) throw new Error("Fant ingen id for tiltaksgjennomføring");

    const dialog = confirm("Er du sikker på at du vil avbryte gjennomføringen?");
    if (dialog) {
      mutation.mutate(gjennomforing.id);
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

  if (
    [
      TiltaksgjennomforingStatus.AVSLUTTET,
      TiltaksgjennomforingStatus.AVBRUTT,
      TiltaksgjennomforingStatus.AVLYST,
    ].includes(gjennomforing?.status!!)
  ) {
    // Trenger ikke avbryt en avtale som allerede er avsluttet
    return null;
  }

  return (
    <div className={styles.warning_container}>
      <ReadMore header="Hva betyr det å avbryte gjennomføringen?">
        <BodyLong>
          Hvis gjennomføringens startdato er passert kan du avbryte gjennomføringen. Den vil da bli
          satt som avbrutt i systemet. Du kan ikke avbryte en gjennomføring som har deltakere
          tilknyttet seg.
        </BodyLong>
      </ReadMore>

      <Button type="button" size="small" variant="danger" onClick={avbrytAvtale}>
        Jeg vil avbryte gjennomføringen
      </Button>
      {error ? (
        <Alert variant="warning">
          <Heading spacing size="small" level="3">
            Klarte ikke avbryte gjennomføring
          </Heading>
          {error}
        </Alert>
      ) : null}
    </div>
  );
}
