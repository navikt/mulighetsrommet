import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Radio, Spacer, TextField, VStack } from "@navikt/ds-react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormGroup } from "@/layouts/FormGroup";
import {
  TilskuddBehandlingRequest,
  TilskuddOpplaeringType,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { KostnadsstedOption, VelgKostnadssted } from "../tilsagn/form/VelgKostnadssted";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { defaultVedtakRequest } from "./defaultVedtakRequest";

export function SaksopplysningerForm() {
  const { control, register } = useFormContext<TilskuddBehandlingRequest>();

  const { fields, append, remove } = useFieldArray({
    control,
    name: "vedtak",
  });

  const kostnadssteder: KostnadsstedOption[] = [
    {
      enhetsnummer: "0213",
      navn: "Nav Nordre Follo",
    },
  ];

  return (
    <>
      <Heading size="small" level="3" spacing>
        Informasjon fra søknad
      </Heading>
      <VStack gap="space-20" align="start">
        <FormTextField label="JournalpostID" name="soknadJournalpostId" />
        <FormDateInput name="soknadDato" label="Søknadsdato" />
        <HStack gap="space-8">
          <FormDateInput name="periodeStart" label="Periodestart" />
          <FormDateInput name="periodeSlutt" label="Periodeslutt" />
        </HStack>
        <VelgKostnadssted kostnadssteder={kostnadssteder} />

        {fields.map((field, index) => (
          <FormGroup key={field.id}>
            <HStack align="center" justify="space-between">
              <VStack gap="space-8">
                <HStack gap="space-24" align="start">
                  <FormSelect label="Tilskuddstype" name={`vedtak.${index}.tilskuddOpplaeringType`}>
                    <option value="">-- Velg tilskuddstype --</option>
                    {Object.keys(TilskuddOpplaeringType).map((tilskudd) => (
                      <option key={tilskudd} value={tilskudd}>
                        {tilskudd}
                      </option>
                    ))}
                  </FormSelect>
                  <TextField
                    size="small"
                    type="text"
                    label="Beløp fra søknad"
                    {...register(`vedtak.${index}.soknadBelop.belop`, {
                      setValueAs: (v: string) => (v === "" ? null : Number(v)),
                      validate: (value: number | null) => {
                        if (!Number.isInteger(value)) return "Beløp må være et heltall";
                        return true;
                      },
                    })}
                  />
                  <FormSelect label="Valuta" name={`vedtak.${index}.soknadBelop.valuta`}>
                    <option value={Valuta.NOK}>NOK</option>
                    <option value={Valuta.SEK}>SEK</option>
                  </FormSelect>
                  <Spacer />
                </HStack>
                <Separator />
                <ControlledRadioGroup
                  size="small"
                  name={`vedtak.${index}.utbetalingMottaker`}
                  legend="Vedtaksresultat"
                  horisontal
                >
                  <Radio value="bruker">Utbetales til brukeren</Radio>
                  <Radio value="arrangor">Utbetales til arrangøren</Radio>
                </ControlledRadioGroup>
              </VStack>
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
          onClick={() => append(defaultVedtakRequest)}
        >
          Legg til tilskudd
        </Button>
      </VStack>
    </>
  );
}
