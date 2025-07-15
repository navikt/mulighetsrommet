import { formaterDato } from "@/utils/Utils";
import { type Oppgave, OppgaveEnhet, OppgaveIconType, OppgaveType } from "@mr/api-client-v2";
import { BankNoteIcon, HandshakeIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { BodyShort, Box, LinkCard, Spacer, Tag, TagProps } from "@navikt/ds-react";
import { ReactNode } from "react";
import { Link } from "react-router";

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
        style={{ backgroundColor: "var(--a-grayalpha-100)" }}
      >
        <LinkCard.Icon>
          <OppgaveIcon type={iconType} fontSize="2rem" />
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
          type={oppgave.type}
          label={oppgave.navn}
          icon={<OppgaveIcon type={iconType} />}
        />
        <OppgaveEnhetTag enhet={oppgave.enhet} />
        <Spacer />
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

function getOppgaveVariant(type: OppgaveType): TagProps["variant"] {
  switch (type) {
    case OppgaveType.TILSAGN_TIL_ANNULLERING:
    case OppgaveType.TILSAGN_TIL_OPPGJOR:
    case OppgaveType.TILSAGN_RETURNERT:
    case OppgaveType.UTBETALING_RETURNERT:
    case OppgaveType.AVTALE_MANGLER_ADMINISTRATOR:
    case OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR:
      return "warning-moderate";
    case OppgaveType.TILSAGN_TIL_GODKJENNING:
    case OppgaveType.UTBETALING_TIL_ATTESTERING:
    case OppgaveType.UTBETALING_TIL_BEHANDLING:
      return "error-moderate";
    default:
      return "warning-moderate";
  }
}

interface OppgaveStatusProps {
  type: OppgaveType;
  label: string;
  icon: ReactNode;
}

function OppgaveStatus({ type, label, icon }: OppgaveStatusProps) {
  const variant = getOppgaveVariant(type);

  return (
    <Tag size="small" variant={variant} icon={icon}>
      {label}
    </Tag>
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
