import { VeilederflateTiltakstype } from "@mr/api-client";
import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";
import { erKurstiltak } from "../../utils/Utils";

interface Props {
  navn: string;
  kurstittel?: string;
  tiltakstype: VeilederflateTiltakstype;
}

export function VisningsnavnForTiltak({ navn, tiltakstype, kurstittel }: Props): ReactNode {
  const { tiltakskode, arenakode } = tiltakstype;
  if (erKurstiltak(tiltakskode, arenakode)) {
    return (
      <div className={styles.container}>
        <BodyShort size="small">{kurstittel ?? navn}</BodyShort>
        <BodyShort size="small">{tiltakstype.navn}</BodyShort>
      </div>
    );
  }
  return (
    <div className={styles.container}>
      <BodyShort size="small">{tiltakstype.navn}</BodyShort>
      <BodyShort size="small">{navn}</BodyShort>
    </div>
  );
}
