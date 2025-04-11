import {
  Alert,
  Button,
  Heading,
  HelpText,
  HStack,
  Link,
  TextField,
  VStack,
} from "@navikt/ds-react";

interface Props {
  kontonummer?: string;
  error?: string;
  onClick: () => void;
}

export function KontonummerInput({ kontonummer, error, onClick }: Props) {
  return (
    <HStack>
      <HStack align="end" gap="2">
        <TextField
          label="Kontonummer"
          size="small"
          description="Kontonummeret hentes automatisk fra Altinn"
          error={error}
          name="kontonummer"
          defaultValue={kontonummer}
          maxLength={11}
          minLength={11}
          id="kontonummer"
          readOnly
        />
        <HStack align="start" gap="2">
          <Button type="button" variant="secondary" size="small" onClick={onClick}>
            Synkroniser kontonummer
          </Button>
          <HelpText>
            Dersom du har oppdatert kontoregisteret via Altinn kan du trykke på knappen "Synkroniser
            kontonummer" for å hente kontonummeret på nytt fra Altinn.
          </HelpText>
        </HStack>
      </HStack>
      <small className="mt-2 block">
        Er kontonummeret feil kan du lese her om <EndreKontonummerLink />.
      </small>
      {!kontonummer ? (
        <Alert variant="warning" className="my-5">
          <VStack align="start" gap="2">
            <Heading spacing size="xsmall" level="3">
              Fant ikke kontonummer
            </Heading>
            <p>
              Vi fant ikke noe kontonummer for din organisasjon. Her kan du lese om{" "}
              <EndreKontonummerLink />.
            </p>
            <p className="text-balance">
              Når du har registrert kontonummer kan du prøve på nytt ved å trykke på knappen{" "}
              <b>Synkroniser kontonummer</b>.
            </p>
          </VStack>
        </Alert>
      ) : null}
    </HStack>
  );
}

function EndreKontonummerLink() {
  return (
    <Link
      rel="noopener noreferrer"
      href="https://www.nav.no/arbeidsgiver/endre-kontonummer#hvordan"
      target="_blank"
    >
      endring av kontonummer for refusjoner fra Nav
    </Link>
  );
}
