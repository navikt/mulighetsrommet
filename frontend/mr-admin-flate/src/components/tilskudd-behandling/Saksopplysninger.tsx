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
import { VelgKostnadssted } from "../tilsagn/form/VelgKostnadssted";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { defaultVedtakRequest } from "./defaultVedtakRequest";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { opplaeringTilskuddToString } from "@/utils/Utils";

export function SaksopplysningerForm() {
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<TilskuddBehandlingRequest>();

  const { fields, append, remove } = useFieldArray({
    control,
    name: "tilskudd",
  });

  const { data: kostnadssteder } = useKostnadssteder();

  function totaltBelop(): number {
    return fields.reduce((sum, v) => sum + (v.soknadBelop?.belop ?? 0), 0);
  }
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
        <VelgKostnadssted
          kostnadssteder={kostnadssteder.flatMap((r) => r.kostnadssteder.map((k) => k))}
        />
        {fields.map((field, index) => (
          <FormGroup key={field.id}>
            <HStack align="center" justify="space-between">
              <VStack gap="space-8">
                <HStack gap="space-24" align="start">
                  <FormSelect
                    label="Tilskuddstype"
                    name={`tilskudd.${index}.tilskuddOpplaeringType`}
                  >
                    <option value="">-- Velg tilskuddstype --</option>
                    {(Object.keys(TilskuddOpplaeringType) as TilskuddOpplaeringType[]).map(
                      (tilskudd) => (
                        <option key={tilskudd} value={tilskudd}>
                          {opplaeringTilskuddToString(tilskudd)}
                        </option>
                      ),
                    )}
                  </FormSelect>
                  <TextField
                    size="small"
                    type="text"
                    label="Beløp fra søknad"
                    error={errors.tilskudd?.[index]?.soknadBelop?.belop?.message}
                    {...register(`tilskudd.${index}.soknadBelop.belop`, {
                      setValueAs: (t: string) => (t === "" ? null : Number(t)),
                      validate: (value: number | null) => {
                        if (!Number.isInteger(value)) return "Beløp må være et heltall";
                        return true;
                      },
                    })}
                  />
                  <FormSelect label="Valuta" name={`tilskudd.${index}.soknadBelop.valuta`}>
                    <option value={Valuta.NOK}>NOK</option>
                    <option value={Valuta.SEK}>SEK</option>
                  </FormSelect>
                  <Spacer />
                </HStack>
                <Separator />
                <ControlledRadioGroup
                  size="small"
                  name={`tilskudd.${index}.utbetalingMottaker`}
                  legend="Hvem skal motta utbetalingen?"
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
        <TextField
          size="small"
          readOnly
          label="Totalt beløp fra søknad"
          value={formaterValutaBelop({
            belop: totaltBelop(),
            valuta: Valuta.NOK,
          })}
        />
      </VStack>
    </>
  );
}
