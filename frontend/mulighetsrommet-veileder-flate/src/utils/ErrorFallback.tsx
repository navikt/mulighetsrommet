import { Melding } from "@/components/melding/Melding";
import { BodyShort, Box, Heading, HStack } from "@navikt/ds-react";
import { FallbackProps } from "react-error-boundary";
import { PortenLink } from "@/components/PortenLink";
import { isProblemDetail } from "@mr/frontend-common/components/error-handling/errors";

export function ErrorFallback({ error }: FallbackProps) {
  return (
    <HStack justify="center" align="center">
      <Melding variant="danger" header="Vi beklager, men noe gikk galt">
        {isProblemDetail(error) ? (
          <>
            <Box marginBlock="space-0 space-8">
              <BodyShort spacing={true}>{error.title}</BodyShort>
              <Heading level="5" size="xsmall">
                Feilmelding
              </Heading>
              <BodyShort size="small">{error.detail}</BodyShort>
              {"traceId" in error && (
                <>
                  <Heading level="5" size="xsmall">
                    Sporingsnøkkel
                  </Heading>
                  <BodyShort size="small">{String(error.traceId)}</BodyShort>
                </>
              )}
            </Box>
            <BodyShort>
              <PortenLink>Meld sak i Porten</PortenLink> hvis problemene vedvarer.
            </BodyShort>
          </>
        ) : (
          <>
            <BodyShort spacing>
              Arbeidsmarkedstiltakene kunne ikke hentes på grunn av en feil hos oss.
            </BodyShort>
            <BodyShort>
              Vennligst last nettsiden på nytt, eller ta <PortenLink>kontakt i Porten</PortenLink>{" "}
              dersom du trenger mer hjelp.
            </BodyShort>
          </>
        )}
      </Melding>
    </HStack>
  );
}
