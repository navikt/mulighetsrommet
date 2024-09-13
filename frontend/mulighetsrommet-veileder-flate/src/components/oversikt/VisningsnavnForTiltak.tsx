import { VeilederflateTiltakstype } from "@mr/api-client";
import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";
import { erKurstiltak } from "../../utils/Utils";
import classNames from "classnames";

interface Props {
  navn: string;
  tiltakstype: Partial<Pick<VeilederflateTiltakstype, "tiltakskode" | "arenakode">> &
    Pick<VeilederflateTiltakstype, "navn">;
  noLink?: boolean;
}

export function VisningsnavnForTiltak({ navn, tiltakstype, noLink = false }: Props): ReactNode {
  const { tiltakskode, arenakode } = tiltakstype;
  if (erKurstiltak(tiltakskode, arenakode)) {
    return (
      <div
        className={classNames(styles.container, {
          [styles.no_link]: noLink,
        })}
      >
        <OriginaltNavn navn={navn} />
        <Tiltaksnavn navn={tiltakstype.navn} />
      </div>
    );
  }
  return (
    <div
      className={classNames(styles.container, {
        [styles.no_link]: noLink,
      })}
    >
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
