import { Box } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client-v2";
import styles from "./SidemenyInfo.module.scss";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function SidemenyKanKombineresMed({ tiltak }: Props) {
  const { tiltakstype } = tiltak;

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
