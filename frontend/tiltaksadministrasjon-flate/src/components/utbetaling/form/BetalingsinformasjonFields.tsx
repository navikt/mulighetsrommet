import { BodyShort, Heading, Link, Loader, LocalAlert, TextField, VStack } from "@navikt/ds-react";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FieldValues, Path } from "react-hook-form";

export function BetalingsinformasjonFields<T extends FieldValues>({
  arrangorId,
  kidNummerName,
}: {
  arrangorId: string;
  kidNummerName: Path<T>;
}) {
  const { data: betalingsinformasjon, isPending } = useArrangorBetalingsinformasjon(arrangorId);

  if (isPending) return <Loader />;

  if (!betalingsinformasjon?.type) {
    return (
      <LocalAlert status="warning">
        <LocalAlert.Header>
          <LocalAlert.Title as="h3">Mangler betalingsinformasjon</LocalAlert.Title>
        </LocalAlert.Header>
        <LocalAlert.Content>
          Arrangøren har ingen betalingsinformasjon registrert i Altinn. Arrangøren må registrere
          kontonummer i Altinn før utbetaling kan behandles. Les mer om <EndreKontonummerLink />.
        </LocalAlert.Content>
      </LocalAlert>
    );
  }

  switch (betalingsinformasjon.type) {
    case "BBan":
      return (
        <VStack gap="space-8" align="start">
          <TextField
            size="small"
            label="Kontonummer til arrangør"
            readOnly
            value={betalingsinformasjon.kontonummer}
            description="Kontonummer hentes automatisk fra Altinn"
          />
          <BodyShort size="small">
            Dersom kontonummer er feil må arrangør oppdatere kontonummer i Altinn. Her kan du lese
            om <EndreKontonummerLink />.
          </BodyShort>
          <FormTextField<T> label="Valgfritt KID-nummer" name={kidNummerName} />
        </VStack>
      );
    case "IBan":
      return (
        <VStack gap="space-8" align="start">
          <Heading size="small">Bank</Heading>
          <TextField size="small" label="IBan" readOnly value={betalingsinformasjon.iban} />
          <TextField size="small" label="BIC/SWIFT" readOnly value={betalingsinformasjon.bic} />
          <TextField size="small" label="Banknavn" readOnly value={betalingsinformasjon.bankNavn} />
          <TextField
            size="small"
            label="Bank landkode"
            readOnly
            value={betalingsinformasjon.bankLandKode}
          />
          <BodyShort size="small">
            Dersom informasjonen må oppdateres ta kontakt med utviklingsteamet.
          </BodyShort>
        </VStack>
      );
  }
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
