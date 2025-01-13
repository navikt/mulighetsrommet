import { BellIcon } from "@navikt/aksel-icons";
import { Heading, Link, LinkPanel } from "@navikt/ds-react";
import styles from "./Oppgave.module.scss";
import { type Oppgave, OppgaveType, Tiltakskode } from "@mr/api-client";
import { formaterDato } from "@/utils/Utils";

interface OppgaveProps {
  oppgave: Oppgave;
}

export function Oppgave({ oppgave }: OppgaveProps) {
  const { title, description, link, frist, createdAt } = oppgave;
  const fristDate = new Date(frist);
  console.log(oppgave.type, fristDate);

  return (
    <>
      <div className={styles.oppgave_container} data-testid="oppgaver">
        <div className={styles.header}>
          <span>Arbeidsforberedende trening</span>
          <span>{formaterDato(fristDate)}</span>
        </div>
        <div>
          <div className={styles.titleAndStatus}>
            <Heading size="medium">{title}</Heading>
            <OppgaveStatus status={oppgave.type} />
          </div>
          <div>{description}</div>
          <Link className={styles.link} href={link.link}>
            {link.linkText}
          </Link>
          {createdAt}
        </div>
      </div>
    </>
  );
}

const labels: Record<string, { label: string; color: string }> = {
  TILSAGN_TIL_BESLUTNING: {
    label: "Tilsagn til annullering",
    color: "#FFD799",
  },
  TILSAGN_TIL_ANNULLERING: {
    label: "Tilsagn til beslutning",
    color: "#CCE2F0",
  },
};

function OppgaveStatus({ status }: { status: OppgaveType }) {
  const { label, color } = labels[status];

  return (
    <div
      style={{
        backgroundColor: color,
      }}
      className={styles.oppgaveStatus}
    >
      {label}
    </div>
  );
}
