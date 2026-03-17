import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import type { BehandlingFormData } from "./schema";
import { FormGroup } from "@/layouts/FormGroup";
import { useOpplaeringtilskudd } from "./useOpplaeringtilskudd";

const tomtTilskudd = {
  tilskuddstype: "",
  belop: "",
  belopTilUtbetaling: "",
  nodvendigForOpplaring: undefined,
  begrunnelse: "",
  vedtaksresultat: undefined,
};

export function Saksopplysninger() {
  const { control } = useFormContext<BehandlingFormData>();
  const { data: opplaeringtilskudd } = useOpplaeringtilskudd();

  const { fields, append, remove } = useFieldArray({
    control,
    name: "tilskudd",
  });

  return (
    <>
      <Heading size="medium" level="3" spacing>
        Opplysninger fra søknad
      </Heading>
      <VStack gap="space-20" align="start">
        <FormTextField
          label="JournalpostID"
          name="journalpostId"
          rules={{ required: "JournalpostID er påkrevd" }}
        />
        <FormDateInput
          name="soknadstidspunkt"
          label="Søknadstidspunkt"
          rules={{ required: "Søknadstidspunkt er påkrevd" }}
        />
        {fields.map((field, index) => (
          <FormGroup key={field.id}>
            <HStack gap="space-24" align="start">
              <FormSelect
                label="Tilskuddstype"
                name={`tilskudd.${index}.tilskuddstype`}
                rules={{ required: "Tilskuddstype er påkrevd" }}
              >
                <option value="">-- Velg tilskuddstype --</option>
                {opplaeringtilskudd.map((tilskudd) => (
                  <option key={tilskudd.id} value={tilskudd.navn}>
                    {tilskudd.navn}
                  </option>
                ))}
              </FormSelect>
              <FormTextField
                label="Beløp"
                name={`tilskudd.${index}.belop`}
                rules={{ required: "Beløp er påkrevd" }}
              />
              <Spacer />
              {fields.length > 1 && (
                <Button
                  size="small"
                  variant="tertiary"
                  data-color="neutral"
                  icon={<TrashIcon aria-hidden />}
                  onClick={() => remove(index)}
                >
                  Fjern
                </Button>
              )}
            </HStack>
          </FormGroup>
        ))}
        <Button
          size="small"
          variant="secondary"
          icon={<PlusIcon aria-hidden />}
          onClick={() => append(tomtTilskudd)}
        >
          Legg til tilskudd
        </Button>
      </VStack>
    </>
  );
}
