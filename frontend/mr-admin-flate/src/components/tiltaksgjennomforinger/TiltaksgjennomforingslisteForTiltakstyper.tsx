import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforinger";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./TiltaksgjennomforingslisteForTiltakstyper.module.scss";
import tiltaksgjennomforingsStyles from "./Tiltaksgjennomforingeroversikt.module.scss";

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
    <div className={styles.tiltaksgjennomforingsliste}>
      <Heading size="medium" level="2">
        Tiltaksgjennomføringer
      </Heading>

      <ul className={tiltaksgjennomforingsStyles.oversikt}>
        {gjennomforingsliste.length === 0 && (
          <Alert variant="info">Ingen tilhørende tiltaksgjennomføringer</Alert>
        )}
        {gjennomforingsliste.map((tiltaksgjennomforing) => (
          <Tiltaksgjennomforingrad
            fagansvarlig
            key={tiltaksgjennomforing.id}
            tiltaksgjennomforing={tiltaksgjennomforing}
          />
        ))}
      </ul>
    </div>
  );
}
