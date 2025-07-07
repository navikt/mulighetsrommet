import { formaterDato } from "@/utils/Utils";
import { type Oppgave, OppgaveEnhet, OppgaveIconType, OppgaveType } from "@mr/api-client-v2";
import { BankNoteIcon, HandshakeIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { BodyShort, Box, HStack, LinkCard, Tag } from "@navikt/ds-react";
import { ReactNode } from "react";

interface OppgaveProps {
  oppgave: Oppgave;
}

export function Oppgave({ oppgave }: OppgaveProps) {
  const { title, description, link, createdAt, iconType } = oppgave;
  return (
    <LinkCard>
      <Box
        asChild
        borderRadius="12"
        padding="space-8"
        style={{ backgroundColor: "var(--ax-bg-moderateA)" }}
      >
        <LinkCard.Icon>
          <div className="inline-flex items-center justify-center p-2 bg-gray-200 rounded-xl">
            <OppgaveIcon type={iconType} fontSize="2rem" />
          </div>
        </LinkCard.Icon>
      </Box>
      <LinkCard.Title>
        <LinkCard.Anchor href={link.link}>{title}</LinkCard.Anchor>
      </LinkCard.Title>
      <LinkCard.Description>{description}</LinkCard.Description>
      <LinkCard.Footer className="flex justify-between items-center">
        <HStack gap="2">
          <OppgaveStatus
            type={oppgave.type}
            label={oppgave.navn}
            icon={<OppgaveIcon type={iconType} />}
          />
          <OppgaveEnhetTag enhet={oppgave.enhet} />
        </HStack>
        <BodyShort textColor="subtle" size="small">
          Opprettet {formaterDato(createdAt)}
        </BodyShort>
      </LinkCard.Footer>
    </LinkCard>
  );
}

function OppgaveIcon(props: { type: OppgaveIconType; fontSize?: string }) {
  switch (props.type) {
    case OppgaveIconType.TILSAGN:
      return <PiggybankIcon fontSize={props.fontSize} />;
    case OppgaveIconType.UTBETALING:
      return <BankNoteIcon fontSize={props.fontSize} />;
    case OppgaveIconType.AVTALE:
    case OppgaveIconType.GJENNOMFORING:
      return <HandshakeIcon fontSize={props.fontSize} />;
  }
}

const POSITIVE_COLOR = "#FFD799";
const NEGATIVE_COLOR = "#FFC2C2";

const oppgaveColor: Record<OppgaveType, string> = {
  TILSAGN_TIL_GODKJENNING: POSITIVE_COLOR,
  TILSAGN_TIL_ANNULLERING: NEGATIVE_COLOR,
  TILSAGN_TIL_OPPGJOR: NEGATIVE_COLOR,
  TILSAGN_RETURNERT: NEGATIVE_COLOR,
  UTBETALING_TIL_ATTESTERING: POSITIVE_COLOR,
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
      className="p-1 h-6 flex items-center gap-2 min-w-[230px] justify-center"
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
  return (
    <Tag size="small" variant="neutral-moderate">
      {enhet.navn}
    </Tag>
  );
}
