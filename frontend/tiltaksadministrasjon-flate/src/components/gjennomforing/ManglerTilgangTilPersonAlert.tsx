import { Alert, BodyShort, Heading, VStack } from "@navikt/ds-react";
import { AvvistGrunn } from "@tiltaksadministrasjon/api-client";
import { EyeSlashIcon } from "@navikt/aksel-icons";
import { ErrorBoundary, FallbackProps } from "react-error-boundary";
import { isProblemDetail } from "@mr/frontend-common/components/error-handling/errors";
import { PropsWithChildren } from "react";

interface Props {
  avvistGrunn: AvvistGrunn | string;
}

export function ManglerTilgangTilPersonAlert({ avvistGrunn }: Props) {
  const { tittel, beskrivelse } = avvistGrunnTekst(avvistGrunn);

  return (
    <Alert variant="warning" role="alert">
      <VStack gap="space-4">
        <Heading size="xsmall" level="3">
          <EyeSlashIcon title="Mangler tilgang" fontSize="1.25rem" aria-hidden /> {tittel}
        </Heading>
        <BodyShort>{beskrivelse}</BodyShort>
      </VStack>
    </Alert>
  );
}

function isManglerTilgangTilPersonError(
  error: unknown,
): error is { type: "mangler-tilgang-til-person"; avvistGrunn: string } {
  return isProblemDetail(error) && error.type === "mangler-tilgang-til-person";
}

function ManglerTilgangTilPersonFallback({ error }: FallbackProps) {
  if (isManglerTilgangTilPersonError(error)) {
    return <ManglerTilgangTilPersonAlert avvistGrunn={error.avvistGrunn as string} />;
  }
  throw error;
}

export function ManglerTilgangTilPersonErrorBoundary({
  children,
  resetKeys,
}: PropsWithChildren<{ resetKeys?: unknown[] }>) {
  return (
    <ErrorBoundary fallbackRender={ManglerTilgangTilPersonFallback} resetKeys={resetKeys}>
      {children}
    </ErrorBoundary>
  );
}

function avvistGrunnTekst(avvistGrunn: AvvistGrunn | string): {
  tittel: string;
  beskrivelse: string;
} {
  switch (avvistGrunn) {
    case AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE:
    case AvvistGrunn.AVVIST_STRENGT_FORTROLIG_UTLAND:
    case AvvistGrunn.AVVIST_FORTROLIG_ADRESSE:
      return {
        tittel: "Personen har adressebeskyttelse",
        beskrivelse:
          "Du har ikke tilgang til å se informasjon om denne personen fordi personen har adressebeskyttelse.",
      };
    case AvvistGrunn.AVVIST_SKJERMING:
      return {
        tittel: "Personen er skjermet",
        beskrivelse:
          "Du har ikke tilgang til å se informasjon om denne personen fordi personen er skjermet (egen ansatt).",
      };
    case AvvistGrunn.AVVIST_HABILITET:
      return {
        tittel: "Habilitetsbegrensning",
        beskrivelse:
          "Du har ikke tilgang til å se informasjon om denne personen på grunn av habilitet.",
      };
    case AvvistGrunn.AVVIST_VERGE:
    case AvvistGrunn.AVVIST_VERGEMAAL:
      return {
        tittel: "Vergemål",
        beskrivelse:
          "Du har ikke tilgang til å se informasjon om denne personen på grunn av vergemål.",
      };
    case AvvistGrunn.AVVIST_AVDOED:
      return {
        tittel: "Personen er død",
        beskrivelse: "Du har ikke tilgang til å se informasjon om denne personen.",
      };
    case AvvistGrunn.AVVIST_GEOGRAFISK:
    case AvvistGrunn.AVVIST_PERSON_UTLAND:
    case AvvistGrunn.AVVIST_UKJENT_BOSTED:
      return {
        tittel: "Geografisk begrensning",
        beskrivelse:
          "Du har ikke tilgang til å se informasjon om denne personen på grunn av geografisk tilknytning.",
      };
    default:
      return {
        tittel: "Mangler tilgang til person",
        beskrivelse: "Du har ikke tilgang til å se informasjon om denne personen.",
      };
  }
}
