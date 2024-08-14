import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { ApiError } from "@mr/api-client";
import { BodyShort, Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import { FallbackProps } from "react-error-boundary";
import { PortenLink } from "@/components/PortenLink";
import { resolveErrorMessage, resolveRequestId } from "../errors";

export function ErrorFallback({ error }: FallbackProps) {
  let feilmelding: ReactNode;
  if (error instanceof ApiError) {
    feilmelding = renderApiError(error);
  } else {
    feilmelding = (
      <>
        <BodyShort>
          Arbeidsmarkedstiltakene kunne ikke hentes på grunn av en feil hos oss.
        </BodyShort>
        <BodyShort>
          Vennligst last nettsiden på nytt, eller ta <PortenLink>kontakt i Porten</PortenLink>{" "}
          dersom du trenger mer hjelp.
        </BodyShort>
      </>
    );
  }

  return (
    <Feilmelding
      ikonvariant="error"
      header={<>Vi beklager, men noe gikk galt</>}
      beskrivelse={feilmelding}
    />
  );
}

function renderApiError(error: ApiError) {
  let description: string;
  if (error.status === 401) {
    description =
      "Det oppstod en feil under behandlingen av forespørselen din. Forsøk å logge ut og inn igjen.";
  } else if (error.status === 404) {
    description =
      "Beklager, siden kan være slettet eller flyttet, eller det var en feil i lenken som førte deg hit.";
  } else {
    description = "Det oppstod en feil under behandlingen av forespørselen din.";
  }

  const message = resolveErrorMessage(error);
  const requestId = resolveRequestId(error);
  return (
    <>
      <BodyShort spacing={true}>{description}</BodyShort>
      {message && (
        <BodyShort as="div" size="small" spacing={true}>
          <Heading level="5" size="small">
            Feilmelding
          </Heading>
          <code>{message}</code>
        </BodyShort>
      )}
      {requestId && (
        <BodyShort as="div" size="small" spacing={true}>
          <Heading level="5" size="small">
            Sporingsnøkkel
          </Heading>
          <code>{requestId}</code>
        </BodyShort>
      )}
      <BodyShort>
        <PortenLink>Meld sak i Porten</PortenLink> hvis problemene vedvarer.
      </BodyShort>
    </>
  );
}
