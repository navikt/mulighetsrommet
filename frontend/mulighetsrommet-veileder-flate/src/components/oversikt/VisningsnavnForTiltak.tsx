import { Tiltakskode, VeilederflateTiltakstype } from "@mr/api-client";
import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";

interface Props {
  navn: string;
  tiltakstype: VeilederflateTiltakstype;
}

function erKurstiltak(tiltakskode: Tiltakskode) {
  return [
    Tiltakskode.JOBBKLUBB,
    Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
    Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
  ].includes(tiltakskode);
}

export function VisningsnavnForTiltak({ navn, tiltakstype }: Props): ReactNode {
  const tiltakskode = tiltakstype.tiltakskode;
  if (tiltakskode && erKurstiltak(tiltakskode)) {
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
