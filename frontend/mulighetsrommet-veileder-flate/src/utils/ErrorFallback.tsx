import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { BodyShort, Heading } from "@navikt/ds-react";
import { FallbackProps } from "react-error-boundary";
import { PortenLink } from "@/components/PortenLink";
import { isProblemDetail } from "@mr/frontend-common/components/error-handling/errors";

export function ErrorFallback({ error }: FallbackProps) {
  return (
    <Feilmelding ikonvariant="error" header={<>Vi beklager, men noe gikk galt</>}>
      {isProblemDetail(error) ? (
        <>
          <BodyShort spacing={true}>{error.title}</BodyShort>
          <BodyShort as="div" size="small" spacing={true}>
            <Heading level="5" size="small">
              Feilmelding
            </Heading>
            <code>{error.detail}</code>
          </BodyShort>
          {"requestId" in error && (
            <BodyShort as="div" size="small" spacing={true}>
              <Heading level="5" size="small">
                Sporingsnøkkel
              </Heading>
              <code>{String(error.requestId)}</code>
            </BodyShort>
          )}
          <BodyShort>
            <PortenLink>Meld sak i Porten</PortenLink> hvis problemene vedvarer.
          </BodyShort>
        </>
      ) : (
        <>
          <BodyShort>
            Arbeidsmarkedstiltakene kunne ikke hentes på grunn av en feil hos oss.
          </BodyShort>
          <BodyShort>
            Vennligst last nettsiden på nytt, eller ta <PortenLink>kontakt i Porten</PortenLink>{" "}
            dersom du trenger mer hjelp.
          </BodyShort>
        </>
      )}
    </Feilmelding>
  );
}
