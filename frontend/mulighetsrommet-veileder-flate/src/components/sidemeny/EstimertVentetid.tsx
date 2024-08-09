import { TimerPauseFillIcon } from "@navikt/aksel-icons";
import { BodyShort } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./EstimertVentetid.module.scss";
import { formatertVentetid } from "@/utils/Utils";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

export function EstimertVentetid({ tiltaksgjennomforing }: Props) {
  if (!tiltaksgjennomforing?.estimertVentetid) {
    return null;
  }

  return (
    <>
      <BodyShort className={styles.container}>
        <TimerPauseFillIcon
          className={styles.ikon}
          aria-label="Stoppeklokkeikon for å indikere estimert ventetid for tiltaket"
        />{" "}
        Estimert ventetid for tiltaket:{" "}
        {formatertVentetid(
          tiltaksgjennomforing.estimertVentetid.verdi,
          tiltaksgjennomforing.estimertVentetid.enhet,
        )}
      </BodyShort>
    </>
  );
}
