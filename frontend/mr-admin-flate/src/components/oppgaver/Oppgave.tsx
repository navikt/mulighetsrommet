import { Heading, Link } from "@navikt/ds-react";
import { type Oppgave, OppgaveIcon, OppgaveType, TiltakstypeDto } from "@mr/api-client-v2";
import { formaterDato } from "@/utils/Utils";
import { PiggybankIcon, BankNoteIcon } from "@navikt/aksel-icons";

interface OppgaveProps {
  oppgave: Oppgave;
  tiltakstype: TiltakstypeDto;
}

export function Oppgave({ oppgave, tiltakstype }: OppgaveProps) {
  const { title, description, link, createdAt, oppgaveIcon } = oppgave;

  return (
    <>
      <div className="bg-white p-4" data-testid="oppgaver">
        <div className="flex justify-between items-center">
          <span>{tiltakstype.navn}</span>
          <OppgaveStatus status={oppgave.type} />
        </div>
        <div>
          <div className="flex justify-between mt-4">
            <Heading size="medium" className="flex items-center gap-2">
              {icon(oppgaveIcon)} {title}
            </Heading>
          </div>
          <div>{description}</div>
          <Link className="mt-3" href={link.link}>
            {link.linkText}
          </Link>
        </div>
        <div className="flex justify-end text-small">
          <span>Opprettet {formaterDato(createdAt)}</span>
        </div>
      </div>
    </>
  );
}

function icon(icon: OppgaveIcon) {
  switch (icon) {
    case OppgaveIcon.TILSAGN:
      return <PiggybankIcon />;
    case OppgaveIcon.UTBETALING:
      return <BankNoteIcon />;
  }
}

const labels: Record<OppgaveType, { label: string; color: string }> = {
  TILSAGN_TIL_GODKJENNING: {
    label: "Tilsagn til godkjenning",
    color: "#FFD799",
  },
  TILSAGN_TIL_ANNULLERING: {
    label: "Tilsagn til annullering",
    color: "#CCE2F0",
  },
  TILSAGN_RETURNERT: {
    label: "Tilsagn returnert",
    color: "#FF9100",
  },
  UTBETALING_TIL_GODKJENNING: {
    label: "Utbetaling til godkjenning",
    color: "#FFD799",
  },
  UTBETALING_RETURNERT: {
    label: "Utbetaling returnert",
    color: "#FF9100",
  },
  UTBETALING_TIL_BEHANDLING: {
    label: "Utbetaling til behandling",
    color: "#FFD799",
  },
};

function OppgaveStatus({ status }: { status: OppgaveType }) {
  const { label, color } = labels[status];

  return (
    <div
      style={{
        backgroundColor: color,
      }}
      className="p-1"
    >
      {label}
    </div>
  );
}
