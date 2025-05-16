import { formaterDato } from "@/utils/Utils";
import { type Oppgave, OppgaveEnhet, OppgaveIcon, OppgaveType } from "@mr/api-client-v2";
import { BankNoteIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { Heading, HStack, Tag } from "@navikt/ds-react";
import { Link } from "react-router";
import { ReactNode } from "react";

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
          <HStack gap="2">
            <OppgaveEnhetTag enhet={oppgave.enhet} />
            <OppgaveStatus type={oppgave.type} label={oppgave.title} icon={icon(oppgaveIcon)} />
          </HStack>
        </div>
        <div>
          <div className="flex justify-between mt-4">
            <Heading size="medium" className="flex items-center gap-2">
              {title}
            </Heading>
          </div>
          <div>{description}</div>
          <Link className="mt-3" to={link.link}>
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

const oppgaveColor: Record<OppgaveType, string> = {
  TILSAGN_TIL_GODKJENNING: POSITIVE_COLOR,
  TILSAGN_TIL_ANNULLERING: NEGATIVE_COLOR,
  TILSAGN_TIL_OPPGJOR: NEGATIVE_COLOR,
  TILSAGN_RETURNERT: NEGATIVE_COLOR,
  UTBETALING_TIL_GODKJENNING: POSITIVE_COLOR,
  UTBETALING_RETURNERT: NEGATIVE_COLOR,
  UTBETALING_TIL_BEHANDLING: POSITIVE_COLOR,
  AVTALE_MANGLER_ADMINISTRATOR: NEGATIVE_COLOR,
  GJENNOMFORING_MANGLER_ADMINISTRATOR: NEGATIVE_COLOR,
};

interface OppgaveStatusProps {
  type: OppgaveType;
  label: string;
  icon: ReactNode;
}

function OppgaveStatus({ type, label, icon }: OppgaveStatusProps) {
  const color = oppgaveColor[type];

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

interface OppgaveEnhetTagProps {
  enhet?: OppgaveEnhet;
}

function OppgaveEnhetTag({ enhet }: OppgaveEnhetTagProps) {
  if (!enhet) {
    return null;
  }
  return <Tag variant="neutral-moderate">{enhet.navn}</Tag>;
}
