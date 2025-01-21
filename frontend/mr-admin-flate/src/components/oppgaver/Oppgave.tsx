import { Heading, Link } from "@navikt/ds-react";
import { type Oppgave, OppgaveType, TiltakstypeDto } from "@mr/api-client-v2";
import { formaterDato } from "@/utils/Utils";

interface OppgaveProps {
  oppgave: Oppgave;
  tiltakstype: TiltakstypeDto;
}

export function Oppgave({ oppgave, tiltakstype }: OppgaveProps) {
  const { title, description, link, deadline } = oppgave;
  const deadlineDate = new Date(deadline);

  return (
    <>
      <div className="bg-white p-4" data-testid="oppgaver">
        <div className="flex justify-between items-center">
          <span>{tiltakstype.navn}</span>
          <span>{formaterDato(deadlineDate)}</span>
        </div>
        <div>
          <div className="flex justify-between mt-4">
            <Heading size="medium">{title}</Heading>
            <OppgaveStatus status={oppgave.type} />
          </div>
          <div>{description}</div>
          <Link className="mt-3" href={link.link}>
            {link.linkText}
          </Link>
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
      className="p-1"
    >
      {label}
    </div>
  );
}
