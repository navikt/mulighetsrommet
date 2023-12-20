import { BodyShort, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { kebabCase } from "../../utils/Utils";
import styles from "./Forsidekort.module.scss";

interface ForsidekortProps {
  navn: string;
  ikon: React.ReactNode;
  url: string;
  tekst?: string;
}
export function Forsidekort({ navn, ikon, url, tekst }: ForsidekortProps) {
  return (
    <Link key={url} className={styles.card} to={url} data-testid={`forsidekort-${kebabCase(navn)}`}>
      <span className={styles.circle}>{ikon}</span>
      <Heading size="medium" level="3">
        {navn}
      </Heading>
      {tekst ? <BodyShort className={styles.infotekst}>{tekst}</BodyShort> : null}
    </Link>
  );
}
