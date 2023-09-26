import { Feilmelding } from "../components/feilmelding/Feilmelding";
import { PORTEN } from "mulighetsrommet-frontend-common/constants";
import { ApiError } from "mulighetsrommet-api-client";
import { BodyShort, Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import { FallbackProps } from "react-error-boundary";

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
          Vennligst last nettsiden på nytt, eller ta <a href={PORTEN}>kontakt i Porten</a> dersom du
          trenger mer hjelp.
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

  return (
    <>
      <BodyShort>{description}</BodyShort>
      {error.body && (
        <p>
          <Heading level="5" size="small">
            Feilmelding
          </Heading>
          <code>{error.body}</code>
        </p>
      )}
      <BodyShort>
        <a href={PORTEN}>Meld sak i Porten</a> hvis problemene vedvarer.
      </BodyShort>
    </>
  );
}
