import { BodyShort, Heading, List, Box, Link } from "@navikt/ds-react";
import { PadlockLockedIcon } from "@navikt/aksel-icons";
import { TILGANGER_DOKUMENTASJON_URL } from "@/constants";
import { ProblemDetail } from "@tiltaksadministrasjon/api-client";

export interface NavAnsattManglerTilgangError extends ProblemDetail {
  type: "mangler-tilgang";
  extensions: {
    missingRoles: string[];
  };
}

interface IngenTilgangProps {
  error: NavAnsattManglerTilgangError;
}

export function IngenTilgang({ error }: IngenTilgangProps) {
  return (
    <main className="w-1/2 m-auto mt-16 flex justify-center text-center prose">
      <div className="bg-ax-bg-default p-4 rounded-md flex flex-col items-center">
        <PadlockLockedIcon fontSize={50} />
        <Heading size="medium">Ingen tilgang</Heading>
        <BodyShort spacing>Du har ikke tilgang til denne siden</BodyShort>
        <BodyShort>{error.detail}</BodyShort>
        <div className="text-left">
          <Box marginBlock="space-16" asChild>
            <List>
              {error.extensions.missingRoles.map((rolle) => (
                <List.Item key={rolle}>
                  <strong>{rolle}</strong>
                </List.Item>
              ))}
            </List>
          </Box>
        </div>
        <BodyShort>Hvis du nettopp har fått tilgang, forsøk å logge ut og inn igjen.</BodyShort>
        <BodyShort spacing>
          Hvis det fortsatt ikke fungerer,{" "}
          <Link href={TILGANGER_DOKUMENTASJON_URL}>besøk Tilganger-siden</Link> for å se hvordan du
          bestiller korrekte tilganger.
        </BodyShort>
      </div>
    </main>
  );
}
