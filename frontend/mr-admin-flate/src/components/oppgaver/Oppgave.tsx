import { Heading, Link } from "@navikt/ds-react";
import { type Oppgave, OppgaveIcon, OppgaveType } from "@mr/api-client-v2";
import { formaterDato } from "@/utils/Utils";
import { BankNoteIcon, PiggybankIcon } from "@navikt/aksel-icons";

interface OppgaveProps {
  oppgave: Oppgave;
}

export function Oppgave({ oppgave }: OppgaveProps) {
  const { title, description, link, createdAt, oppgaveIcon } = oppgave;

  return (
    <>
      <div className="bg-white p-4" data-testid="oppgaver">
        <div className="flex justify-between items-center">
          <span>{oppgave.tiltakstype.navn}</span>
          <OppgaveStatus status={oppgave.type} icon={icon(oppgaveIcon)} />
        </div>
        <div>
          <div className="flex justify-between mt-4">
            <Heading size="medium" className="flex items-center gap-2">
              {title}
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

const POSITIVE_COLOR = "#FFD799";
const NEGATIVE_COLOR = "#FFC2C2";

const labels: Record<OppgaveType, { label: string; color: string }> = {
  TILSAGN_TIL_GODKJENNING: {
    label: "Tilsagn til godkjenning",
    color: POSITIVE_COLOR,
  },
  TILSAGN_TIL_ANNULLERING: {
    label: "Tilsagn til annullering",
    color: NEGATIVE_COLOR,
  },
  TILSAGN_TIL_OPPGJOR: {
    label: "Tilsagn til oppgjør",
    color: NEGATIVE_COLOR,
  },
  TILSAGN_RETURNERT: {
    label: "Tilsagn returnert",
    color: NEGATIVE_COLOR,
  },
  UTBETALING_TIL_GODKJENNING: {
    label: "Utbetaling til godkjenning",
    color: POSITIVE_COLOR,
  },
  UTBETALING_RETURNERT: {
    label: "Utbetaling returnert",
    color: NEGATIVE_COLOR,
  },
  UTBETALING_TIL_BEHANDLING: {
    label: "Utbetaling til behandling",
    color: POSITIVE_COLOR,
  },
};

function OppgaveStatus({ status, icon }: { status: OppgaveType; icon: React.ReactNode }) {
  const { label, color } = labels[status];

  return (
    <div
      style={{
        backgroundColor: color,
      }}
      className="p-1 flex items-center gap-2 min-w-[230px] justify-center"
    >
      {icon} {label}
    </div>
  );
}
