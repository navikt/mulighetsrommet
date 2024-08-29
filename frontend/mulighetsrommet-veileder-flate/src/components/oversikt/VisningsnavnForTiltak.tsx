import { VeilederflateTiltakstype } from "@mr/api-client";
import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";
import { erKurstiltak } from "../../utils/Utils";

interface Props {
  navn: string;
  tiltakstype: VeilederflateTiltakstype;
}

export function VisningsnavnForTiltak({ navn, tiltakstype }: Props): ReactNode {
  const { tiltakskode, arenakode } = tiltakstype;
  if (erKurstiltak(tiltakskode, arenakode)) {
    return (
      <div className={styles.container}>
        <OriginaltNavn navn={navn} />
        <Tiltaksnavn navn={tiltakstype.navn} />
      </div>
    );
  }
  return (
    <div className={styles.container}>
      <Tiltaksnavn navn={tiltakstype.navn} />
      <OriginaltNavn navn={navn} />
    </div>
  );
}

function Tiltaksnavn({ navn }: { navn: string }) {
  return (
    <BodyShort size="small" title={navn}>
      {navn}
    </BodyShort>
  );
}

function OriginaltNavn({ navn }: { navn: string }) {
  return <BodyShort size="small">{navn}</BodyShort>;
}
