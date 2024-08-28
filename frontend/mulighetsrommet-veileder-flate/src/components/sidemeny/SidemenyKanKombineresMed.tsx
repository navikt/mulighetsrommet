import { Box } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client";
import styles from "./SidemenyInfo.module.scss";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltak;
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
