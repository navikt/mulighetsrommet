import {
  BodyShort,
  Button,
  HelpText,
  HStack,
  Link,
  LocalAlert,
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
    <VStack gap="space-8" align="start">
      {!kontonummer ? (
        <LocalAlert status="error" size="small">
          <LocalAlert.Header>
            <LocalAlert.Title as="h4">Fant ikke kontonummer</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            <BodyShort spacing>
              Vi fant ikke noe kontonummer for din organisasjon. Her kan du lese om{" "}
              <EndreKontonummerLink />.
            </BodyShort>
            <BodyShort>
              Når du har registrert kontonummer kan du prøve på nytt ved å trykke på knappen{" "}
              <b>"Synkroniser kontonummer"</b>.
            </BodyShort>
          </LocalAlert.Content>
        </LocalAlert>
      ) : null}
      <HStack gap="space-8" align="end">
        <TextField
          label="Kontonummer"
          size="small"
          description="Kontonummeret hentes automatisk"
          error={error}
          name="kontonummer"
          defaultValue={kontonummer}
          htmlSize={35}
          maxLength={11}
          minLength={11}
          id="kontonummer"
          readOnly
        />
        <HStack align="center" gap="space-8">
          <Button type="button" variant="secondary" size="small" onClick={onClick}>
            Synkroniser kontonummer
          </Button>
          <HelpText>
            Dersom du har oppdatert kontoregisteret, kan du trykke på knappen "Synkroniser
            kontonummer" for å hente kontonummeret på nytt.
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
      endring av kontonummer for utbetalinger fra Nav
    </Link>
  );
}
