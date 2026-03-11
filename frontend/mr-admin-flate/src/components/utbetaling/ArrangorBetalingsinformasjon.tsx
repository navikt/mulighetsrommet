import { Betalingsinformasjon, OpprettUtbetalingRequest } from "@tiltaksadministrasjon/api-client";
import { Heading, Link, TextField, VStack } from "@navikt/ds-react";
import { TextInput } from "@/components/skjema/TextInput";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";

interface Props {
  arrangorId: string;
}

export function ArrangorBetalingsinformasjon({ arrangorId }: Props) {
  const { data: betalingsinformasjon } = useArrangorBetalingsinformasjon(arrangorId);
  return (
    <>
      <Heading size="small" level="2">
        Betalingsinformasjon
      </Heading>
      <BetalingsinformasjonView betalingsinformasjon={betalingsinformasjon} />
    </>
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

function BetalingsinformasjonView({
  betalingsinformasjon,
}: {
  betalingsinformasjon: Betalingsinformasjon;
}) {
  switch (betalingsinformasjon.type) {
    case "BBan":
      return (
        <VStack gap="space-8">
          <TextField
            size="small"
            label="Kontonummer til arrangør"
            readOnly
            value={betalingsinformasjon.kontonummer}
            description="Kontonummer hentes automatisk fra Altinn"
          />
          <small className="text-balance">
            Dersom kontonummer er feil må arrangør oppdatere kontonummer i Altinn. Les mer her om{" "}
            <EndreKontonummerLink />.
          </small>
          <TextInput<OpprettUtbetalingRequest> label="Valgfritt KID-nummer" name="kidNummer" />
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
            Dersom informasjonen må oppdateres ta kontakt med team Valp.
          </small>
        </VStack>
      );
    case undefined:
      throw Error("unreachable");
  }
}
