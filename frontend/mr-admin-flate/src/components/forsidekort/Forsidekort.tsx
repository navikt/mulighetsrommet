import styles from "./Forsidekort.module.scss";
import { Link } from "react-router-dom";
import { faro } from "@grafana/faro-web-sdk";
import { BodyShort, Heading } from "@navikt/ds-react";

interface ForsidekortProps {
  navn: string;
  ikon: React.ReactNode;
  url: string;
  tekst?: string;
}
export function Forsidekort({ navn, ikon, url, tekst }: ForsidekortProps) {
  return (
    <Link
      key={url}
      className={styles.card}
      to={url}
      onClick={() =>
        faro?.api?.pushEvent(
          `Bruker trykket på inngang fra forside: ${navn}`,
          {
            inngang: navn,
          },
          "forside",
        )
      }
    >
      <span className={styles.circle}>{ikon}</span>
      <Heading size="medium" level="3">
        {navn}
      </Heading>
      {tekst ? <BodyShort className={styles.infotekst}>{tekst}</BodyShort> : null}
    </Link>
  );
}
