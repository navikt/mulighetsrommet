import { BodyShort, Heading } from "@navikt/ds-react";
import { PadlockLockedIcon } from "@navikt/aksel-icons";
import { TILGANGER_DOKUMENTASJON_URL } from "@/constants";

interface IngenTilgangProps {
  message?: string;
}

export function IngenTilgang({ message }: IngenTilgangProps) {
  return (
    <main className="w-1/2 m-auto mt-16 flex justify-center text-center prose">
      <div className="bg-white p-4 rounded-md flex flex-col items-center">
        <PadlockLockedIcon fontSize={50} />
        <Heading size="medium">Ingen tilgang</Heading>
        <BodyShort spacing>Du har ikke tilgang til denne siden</BodyShort>
        {message && <BodyShort spacing>{message}</BodyShort>}
        <BodyShort spacing>
          <a href={TILGANGER_DOKUMENTASJON_URL}>Gå til Tilganger-siden</a> for å se hvordan du
          bestiller korrekte tilganger.
        </BodyShort>
      </div>
    </main>
  );
}
