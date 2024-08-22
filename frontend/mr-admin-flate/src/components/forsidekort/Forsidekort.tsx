import { BodyShort, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";
import styles from "./Forsidekort.module.scss";
import { logEvent } from "@/logging/amplitude";
import { ReactNode } from "react";

interface ForsidekortProps {
  navn: string;
  ikon: ReactNode;
  url: string;
  apneINyTab?: boolean;
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

export function Forsidekort({ navn, ikon, url, tekst, apneINyTab = false }: ForsidekortProps) {
  return (
    <Link
      key={url}
      onClick={() => loggKlikkPaKort(navn)}
      className={styles.card}
      to={url}
      {...(apneINyTab ? { target: "_blank", rel: "noopener noreferrer" } : {})}
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
