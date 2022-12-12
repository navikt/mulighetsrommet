import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforinger";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";

interface TiltaksgjennomforingerListeTiltakstypeProps {
  tiltakstypeKode: string;
}

export function TiltaksgjennomforingslisteForTiltakstyper({
  tiltakstypeKode,
}: TiltaksgjennomforingerListeTiltakstypeProps) {
  const { data, isLoading } = useTiltaksgjennomforinger();
  if (isLoading) {
    return <Loader size="xlarge" />;
  }
  if (!data) {
    return null;
  }
  const { data: tiltaksgjennomforinger } = data;

  const gjennomforingsliste: Tiltaksgjennomforing[] = [];

  tiltaksgjennomforinger.map((tiltaksgjennomforing) => {
    tiltaksgjennomforing.tiltakskode === tiltakstypeKode &&
      gjennomforingsliste.push(tiltaksgjennomforing);
  });

  return (
    <>
      <Heading size="large" level="2">
        Tiltaksgjennomføringer
      </Heading>

      <ul className={styles.oversikt}>
        {gjennomforingsliste.length === 0 && (
          <Alert variant="info">Ingen tilhørende tiltaksgjennomføringer</Alert>
        )}
        {gjennomforingsliste.map((tiltaksgjennomforing) => (
          <Tiltaksgjennomforingrad
            fagAnsvarlig
            key={tiltaksgjennomforing.id}
            tiltaksgjennomforing={tiltaksgjennomforing}
          />
        ))}
      </ul>
    </>
  );
}
