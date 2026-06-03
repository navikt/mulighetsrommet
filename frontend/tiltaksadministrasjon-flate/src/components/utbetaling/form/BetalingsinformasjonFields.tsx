import { Heading, Link, TextField, VStack } from "@navikt/ds-react";
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
  const { data: betalingsinformasjon } = useArrangorBetalingsinformasjon(arrangorId);

  switch (betalingsinformasjon.type) {
    case "BBan":
      return (
        <VStack gap="space-8">
          <VStack align="start">
            <TextField
              className="w"
              size="small"
              label="Kontonummer til arrangør"
              readOnly
              value={betalingsinformasjon.kontonummer}
              description="Kontonummer hentes automatisk fra Altinn"
            />
          </VStack>
          <small className="text-balance">
            Dersom kontonummer er feil må arrangør oppdatere kontonummer i Altinn. Les mer her om{" "}
            <EndreKontonummerLink />.
          </small>
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
          <small className="text-balance">
            Dersom informasjonen må oppdateres ta kontakt med utviklingsteamet.
          </small>
        </VStack>
      );
    case undefined:
      throw Error("unreachable");
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
