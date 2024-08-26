import { VeilederflateTiltakstype } from "@mr/api-client";
import { BodyShort } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";

interface Props {
  navn: string;
  tiltakstype: VeilederflateTiltakstype;
}

export function VisningsnavnForTiltak({ navn, tiltakstype }: Props): ReactNode {
  return (
    <>
      <Tiltaksnavn navn={tiltakstype.navn} />
      <OriginaltNavn navn={navn} />
    </>
  );
}

function Tiltaksnavn({ navn }: { navn: string }) {
  return (
    <BodyShort size="small" title={navn} className={classNames(styles.truncate, styles.as_link)}>
      {navn}
    </BodyShort>
  );
}

function OriginaltNavn({ navn }: { navn: string }) {
  return <BodyShort className={styles.muted}>{navn}</BodyShort>;
}
