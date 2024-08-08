import { BodyShort } from "@navikt/ds-react";
import classNames from "classnames";
import {
  VeilederflateTiltakstype,
  VeilderflateArrangor,
  Tiltakskode,
} from "mulighetsrommet-api-client";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";
import { lesbareTiltaksnavn } from "../../utils/Utils";

interface Props {
  navn: string;
  tiltakstype: VeilederflateTiltakstype;
  arrangor?: VeilderflateArrangor;
}

export function VisningsnavnForTiltak({ navn, tiltakstype, arrangor }: Props): ReactNode {
  const { tiltakskode } = tiltakstype;

  const tiltakMedKonstruerteNavn = [
    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    Tiltakskode.OPPFOLGING,
    Tiltakskode.AVKLARING,
    Tiltakskode.ARBEIDSRETTET_REHABILITERING,
    Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
    Tiltakskode.JOBBKLUBB,
  ];

  if (tiltakskode && tiltakMedKonstruerteNavn.includes(tiltakskode)) {
    return (
      <>
        <Tiltaksnavn navn={lesbareTiltaksnavn(navn, tiltakstype, arrangor)} />
        <OriginaltNavn navn={navn} />
      </>
    );
  } else {
    return (
      <>
        <Tiltaksnavn navn={navn} />
        <ArrangorNavn arrangor={arrangor} />
        <OriginaltNavn navn={navn} />
      </>
    );
  }
}

function Tiltaksnavn({ navn }: { navn: string }) {
  return (
    <BodyShort size="small" title={navn} className={classNames(styles.truncate, styles.as_link)}>
      {navn}
    </BodyShort>
  );
}

function ArrangorNavn({ arrangor }: { arrangor?: VeilderflateArrangor }) {
  if (!arrangor) return null;

  return (
    <BodyShort size="small" title={arrangor?.selskapsnavn} className={styles.muted}>
      {arrangor?.selskapsnavn}
    </BodyShort>
  );
}

function OriginaltNavn({ navn }: { navn: string }) {
  return <BodyShort className={styles.muted}>{navn}</BodyShort>;
}
