import { type Oppgave, OppgaveEnhet, OppgaveType } from "@tiltaksadministrasjon/api-client";
import { formaterDato } from "@mr/frontend-common/utils/date";
import {
  BankNoteIcon,
  GavelSoundBlockIcon,
  HandshakeIcon,
  PiggybankIcon,
} from "@navikt/aksel-icons";
import { BodyShort, Box, LinkCard, Spacer, Tag, TagProps } from "@navikt/ds-react";
import { ReactNode } from "react";
import { Link } from "react-router";

interface OppgaveProps {
  oppgave: Oppgave;
}

export function Oppgave({ oppgave }: OppgaveProps) {
  const { title, navn, type, description, link, createdAt } = oppgave;
  return (
    <LinkCard>
      <Box
        asChild
        borderRadius="12"
        padding="space-8"
        style={{ backgroundColor: "var(--a-grayalpha-100)" }}
      >
        <LinkCard.Icon>
          <OppgaveIcon type={type} fontSize="2rem" />
        </LinkCard.Icon>
      </Box>
      <LinkCard.Title>
        <LinkCard.Anchor asChild>
          <Link to={link.link}>{title}</Link>
        </LinkCard.Anchor>
      </LinkCard.Title>
      <LinkCard.Description>{description}</LinkCard.Description>
      <LinkCard.Footer>
        <OppgaveStatus
          variant={getOppgaveVariant(type)}
          label={navn}
          icon={<OppgaveIcon type={type} />}
        />
        {oppgave.enhet && <OppgaveEnhetTag enhet={oppgave.enhet} />}
        <Spacer />
        <BodyShort textColor="subtle" size="small">
          Opprettet {formaterDato(createdAt)}
        </BodyShort>
      </LinkCard.Footer>
    </LinkCard>
  );
}

function OppgaveIcon({ type, fontSize }: { type: OppgaveType; fontSize?: string }) {
  switch (type) {
    case OppgaveType.AVTALE_MANGLER_ADMINISTRATOR:
    case OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR:
      return <HandshakeIcon fontSize={fontSize} />;
    case OppgaveType.TILSAGN_TIL_OPPGJOR:
    case OppgaveType.TILSAGN_TIL_ANNULLERING:
    case OppgaveType.TILSAGN_RETURNERT:
    case OppgaveType.TILSAGN_TIL_GODKJENNING:
      return <BankNoteIcon fontSize={fontSize} />;
    case OppgaveType.UTBETALING_RETURNERT:
    case OppgaveType.UTBETALING_TIL_BEHANDLING:
    case OppgaveType.UTBETALING_TIL_ATTESTERING:
      return <PiggybankIcon fontSize={fontSize} />;
  }
}

function getOppgaveVariant(type: OppgaveType) {
  switch (type) {
    case OppgaveType.TILSAGN_TIL_OPPGJOR:
    case OppgaveType.AVTALE_MANGLER_ADMINISTRATOR:
    case OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR:
    case OppgaveType.TILSAGN_TIL_GODKJENNING:
      return "warning";
    case OppgaveType.TILSAGN_TIL_ANNULLERING:
    case OppgaveType.TILSAGN_RETURNERT:
    case OppgaveType.UTBETALING_RETURNERT:
      return "error";
    case OppgaveType.UTBETALING_TIL_ATTESTERING:
      return "info";
    case OppgaveType.UTBETALING_TIL_BEHANDLING:
      return "success";
  }
}

interface OppgaveStatusProps {
  variant: TagProps["variant"];
  label: string;
  icon: ReactNode;
}

function OppgaveStatus({ variant, label, icon }: OppgaveStatusProps) {
  return (
    <Tag size="small" variant={variant} icon={icon}>
      {label}
    </Tag>
  );
}

function OppgaveEnhetTag({ enhet }: { enhet: OppgaveEnhet }) {
  return (
    <Tag size="small" variant="neutral-moderate">
      {enhet.navn}
    </Tag>
  );
}
