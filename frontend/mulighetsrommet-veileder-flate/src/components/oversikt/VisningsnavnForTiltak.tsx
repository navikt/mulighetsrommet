import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";
import { alternativNavn, visningNavn } from "@/utils/Utils";
import { VeilederflateTiltak } from "@mr/api-client";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function VisningsnavnForTiltak({ tiltak }: Props): ReactNode {
  return (
    <div className={styles.container}>
      <BodyShort size="small">{visningNavn(tiltak)}</BodyShort>
      <BodyShort size="small">{alternativNavn(tiltak)}</BodyShort>
    </div>
  );
}
