import { formaterDato } from "@/utils/Utils";
import { type Oppgave, OppgaveEnhet, OppgaveIcon, OppgaveType } from "@mr/api-client-v2";
import { BankNoteIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { BodyShort, Box, Button, Heading, HStack, Spacer, Tag } from "@navikt/ds-react";
import { Link } from "react-router";
import { ReactNode } from "react";

interface OppgaveProps {
  oppgave: Oppgave;
}

export function Oppgave({ oppgave }: OppgaveProps) {
  const { title, description, link, createdAt, oppgaveIcon } = oppgave;

  return (
    <Box background="bg-default" padding="4" data-testid="oppgaver">
      <HStack gap="2">
        <BodyShort>{oppgave.tiltakstype.navn}</BodyShort>
        <Spacer />
        <OppgaveEnhetTag enhet={oppgave.enhet} />
        <OppgaveStatus type={oppgave.type} label={oppgave.title} icon={icon(oppgaveIcon)} />
      </HStack>
      <Heading size="medium" spacing>
        {title}
      </Heading>
      <BodyShort>{description}</BodyShort>
      <HStack align="end">
        <BodyShort>Opprettet {formaterDato(createdAt)}</BodyShort>
        <Spacer />
        <Button as={Link} size="small" className="mt-3" to={link.link}>
          {link.linkText}
        </Button>
      </HStack>
    </Box>
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
