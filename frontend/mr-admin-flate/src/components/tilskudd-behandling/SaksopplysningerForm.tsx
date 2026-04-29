import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Radio, Spacer, TextField, VStack } from "@navikt/ds-react";
import { Path, useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormGroup } from "@/layouts/FormGroup";
import {
  TilskuddBehandlingRequest,
  TilskuddBehandlingRequestTilskuddVedtakRequest,
  TilskuddOpplaeringType,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { VelgKostnadssted } from "../tilsagn/form/VelgKostnadssted";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { defaultVedtakRequest } from "./defaultVedtakRequest";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { BetalingsinformasjonFields } from "../utbetaling/form/BetalingsinformasjonFields";

interface Props {
  arrangorId: string;
}

export function SaksopplysningerForm({ arrangorId }: Props) {
  const {
    control,
    register,
    watch,
    formState: { errors },
  } = useFormContext<TilskuddBehandlingRequest>();

  const { fields, append, remove } = useFieldArray({
    control,
    name: "vedtak",
  });

  const { data: kostnadssteder } = useKostnadssteder();

  function totaltBelop(): number {
    return watch("vedtak").reduce((sum, v) => sum + (v.soknadBelop?.belop ?? 0), 0);
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
                    error={errors.vedtak?.[index]?.soknadBelop?.belop?.message}
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
                  legend="Hvem skal motta utbetalingen"
                  horisontal
                >
                  <Radio value="bruker">Utbetales til brukeren</Radio>
                  <Radio value="arrangor">Utbetales til arrangøren</Radio>
                </ControlledRadioGroup>
                {watch("vedtak")[index].utbetalingMottaker === "arrangor" && (
                  <BetalingsinformasjonFields<TilskuddBehandlingRequestTilskuddVedtakRequest>
                    arrangorId={arrangorId}
                    kidNummerName={
                      `vedtak.${index}.kidNummer` as Path<TilskuddBehandlingRequestTilskuddVedtakRequest>
                    }
                  />
                )}
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
          type="button"
          variant="secondary"
          icon={<PlusIcon aria-hidden />}
          onClick={() => append(defaultVedtakRequest())}
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
