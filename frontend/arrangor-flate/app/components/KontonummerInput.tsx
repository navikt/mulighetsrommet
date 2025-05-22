import {
  Alert,
  BodyShort,
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
    <VStack gap="4">
      {!kontonummer ? (
        <Alert variant="warning">
          <VStack align="start" gap="2">
            <Heading spacing size="xsmall" level="3">
              Fant ikke kontonummer
            </Heading>
            <BodyShort>
              Vi fant ikke noe kontonummer for din organisasjon. Her kan du lese om{" "}
              <EndreKontonummerLink />.
            </BodyShort>
            <BodyShort className="text-balance">
              Når du har registrert kontonummer kan du prøve på nytt ved å trykke på knappen{" "}
              <b>Synkroniser kontonummer</b>.
            </BodyShort>
          </VStack>
        </Alert>
      ) : null}
      <HStack align="end">
        <TextField
          label="Kontonummer"
          size="small"
          description="Kontonummeret hentes automatisk fra Altinn"
          error={error}
          name="kontonummer"
          defaultValue={kontonummer}
          htmlSize={35}
          maxLength={11}
          minLength={11}
          id="kontonummer"
          readOnly
        />
        <HStack align="center" gap="2">
          <Button type="button" variant="secondary" size="small" onClick={onClick}>
            Synkroniser kontonummer
          </Button>
          <HelpText>
            Dersom du har oppdatert kontoregisteret via Altinn kan du trykke på knappen "Synkroniser
            kontonummer" for å hente kontonummeret på nytt fra Altinn.
          </HelpText>
        </HStack>
      </HStack>
      <BodyShort size="small">
        Er kontonummeret feil kan du lese her om <EndreKontonummerLink />.
      </BodyShort>
    </VStack>
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
