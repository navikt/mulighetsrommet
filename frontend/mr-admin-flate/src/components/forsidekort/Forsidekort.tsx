import { BodyShort, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { kebabCase } from "mulighetsrommet-frontend-common/utils/TestUtils";
import styles from "./Forsidekort.module.scss";
import { logEvent } from "@/logging/amplitude";
import { ReactNode } from "react";

interface ForsidekortProps {
  navn: string;
  ikon: ReactNode;
  url: string;
  tekst?: string;
}

function loggKlikkPaKort(forsidekort: string) {
  logEvent({
    name: "tiltaksadministrasjon.klikk-forsidekort",
    data: {
      forsidekort,
    },
  });
}

export function Forsidekort({ navn, ikon, url, tekst }: ForsidekortProps) {
  return (
    <Link
      key={url}
      onClick={() => loggKlikkPaKort(navn)}
      className={styles.card}
      to={url}
      data-testid={`forsidekort-${kebabCase(navn)}`}
    >
      <span className={styles.circle}>{ikon}</span>
      <Heading size="medium" level="3">
        {navn}
      </Heading>
      {tekst ? <BodyShort className={styles.infotekst}>{tekst}</BodyShort> : null}
    </Link>
  );
}
