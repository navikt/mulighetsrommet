import { Box } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./SidemenyInfo.module.scss";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

export function SidemenyKanKombineresMed({ tiltaksgjennomforing }: Props) {
  const { tiltakstype } = tiltaksgjennomforing;

  return (
    <Box padding="5" background="bg-subtle" className={styles.panel} id="sidemeny">
      <ul>
        {tiltakstype.kanKombineresMed.sort().map((tiltakstypen) => (
          <li key={tiltakstypen}>{tiltakstypen}</li>
        ))}
      </ul>
    </Box>
  );
}
