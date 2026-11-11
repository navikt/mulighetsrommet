import {
  BodyShort,
  ErrorMessage,
  Heading,
  InfoCard,
  Link,
  Loader,
  Radio,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FieldError, FieldValues, get, Path, useFormContext, useFormState } from "react-hook-form";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import { ArrangorReferanseType, BBan } from "@tiltaksadministrasjon/api-client";

interface BetalingsinformasjonFieldsProps<T extends FieldValues> {
  arrangorId: string;
  referanseName: Path<T>;
  kidNummerName: Path<T>;
  fakturanummerName: Path<T>;
}

export function BetalingsinformasjonFields<T extends FieldValues>(
  props: BetalingsinformasjonFieldsProps<T>,
) {
  const { data: betalingsinformasjon, isPending } = useArrangorBetalingsinformasjon(
    props.arrangorId,
  );

  // Referanse-feltet vises bare for BBan. For andre tilfeller må feilen vises her,
  // ellers forsvinner valideringsfeil fra backend uten tilbakemelding.
  const { errors } = useFormState<T>({ name: props.referanseName });
  const referanseError = (get(errors, props.referanseName) as FieldError | undefined)?.message;

  if (isPending) return <Loader />;

  if (!betalingsinformasjon?.type) {
    return (
      <VStack gap="space-8">
        <InfoCard data-color="warning">
          <InfoCard.Header>
            <InfoCard.Title as="h3">Mangler betalingsinformasjon</InfoCard.Title>
          </InfoCard.Header>
          <InfoCard.Content>
            Arrangøren har ingen betalingsinformasjon registrert i Altinn. Arrangøren må registrere
            kontonummer i Altinn før utbetaling kan behandles. Les mer om <EndreKontonummerLink />.
          </InfoCard.Content>
        </InfoCard>
        {referanseError && <ErrorMessage size="small">{referanseError}</ErrorMessage>}
      </VStack>
    );
  }

  switch (betalingsinformasjon.type) {
    case "BBan":
      return <BbanForm bban={betalingsinformasjon} {...props} />;
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
          {referanseError && <ErrorMessage size="small">{referanseError}</ErrorMessage>}
        </VStack>
      );
  }
}

interface BbanFormProps<T extends FieldValues> {
  bban: BBan;
  referanseName: Path<T>;
  kidNummerName: Path<T>;
  fakturanummerName: Path<T>;
}

function BbanForm<T extends FieldValues>({
  bban,
  referanseName,
  kidNummerName,
  fakturanummerName,
}: BbanFormProps<T>) {
  const { watch } = useFormContext<T>();
  const referanse = watch(referanseName);
  return (
    <VStack gap="space-8" align="start">
      <TextField
        size="small"
        label="Kontonummer til arrangør"
        readOnly
        value={bban.kontonummer}
        description="Kontonummer hentes automatisk fra Altinn"
      />
      <BodyShort size="small">
        Dersom kontonummer er feil må arrangør oppdatere kontonummer i Altinn. Her kan du lese om{" "}
        <EndreKontonummerLink />.
      </BodyShort>
      <Separator />
      <ControlledRadioGroup
        size="small"
        name={referanseName}
        legend="Hvordan skal utbetalingen merkes?"
        horisontal
      >
        <Radio value={ArrangorReferanseType.KID}>KID-nummer</Radio>
        <Radio value={ArrangorReferanseType.FAKTURANUMMER}>Fakturanummer</Radio>
      </ControlledRadioGroup>
      {referanse === ArrangorReferanseType.KID && (
        <FormTextField<T> label="KID-nummer" name={kidNummerName} />
      )}
      {referanse === ArrangorReferanseType.FAKTURANUMMER && (
        <FormTextField<T> label="Fakturanummer" name={fakturanummerName} />
      )}
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
