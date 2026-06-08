import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Radio, Spacer, TextField, VStack } from "@navikt/ds-react";
import { Path, useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormGroup } from "@/layouts/FormGroup";
import {
  OpplaeringtilskuddKode,
  TilskuddBehandlingRequest,
  TilskuddBehandlingRequestTilskuddRequest,
  TilskuddMottaker,
  Valuta,
  ValutaBelop,
} from "@tiltaksadministrasjon/api-client";
import { VelgKostnadssted } from "../tilsagn/form/VelgKostnadssted";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { BetalingsinformasjonFields } from "../utbetaling/form/BetalingsinformasjonFields";
import { opplaeringTilskuddToString, tilskuddMottakerToString } from "@/utils/Utils";
import { defaultTilskuddRequest } from "./defaultTilskuddRequest";
import { TotaltBelopBox } from "./TotaltBelopBox";

interface Props {
  arrangorId: string;
}

export function SaksopplysningerForm({ arrangorId }: Props) {
  const {
    control,
    watch,
    register,
    formState: { errors },
  } = useFormContext<TilskuddBehandlingRequest>();

  const { fields, append, remove } = useFieldArray({
    control,
    name: "tilskudd",
  });

  const { data: kostnadssteder } = useKostnadssteder();

  function totaltBelop(): ValutaBelop {
    return {
      belop: fields.reduce((sum, v) => sum + (v.soknadBelop?.belop ?? 0), 0),
      valuta: fields.at(0)?.soknadBelop?.valuta ?? Valuta.NOK,
    };
  }

  return (
    <>
      <Heading size="small" level="3" spacing>
        Informasjon fra søknad
      </Heading>
      <VStack gap="space-20" align="start">
        <FormTextField label="Journalpost-ID" name="soknadJournalpostId" required />
        <FormDateInput name="soknadDato" label="Søknadsdato" required />
        <HStack gap="space-8">
          <FormDateInput name="periodeStart" label="Periodestart" required />
          <FormDateInput name="periodeSlutt" label="Periodeslutt" required />
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
                    required
                  >
                    <option value="">-- Velg tilskuddstype --</option>
                    {(Object.keys(OpplaeringtilskuddKode) as OpplaeringtilskuddKode[]).map(
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
                  <FormSelect
                    size="small"
                    label="Valuta"
                    name={`tilskudd.${index}.soknadBelop.valuta`}
                    required
                    readOnly
                  >
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
                  <Radio value={TilskuddMottaker.BRUKER}>
                    {tilskuddMottakerToString(TilskuddMottaker.BRUKER)}
                  </Radio>
                  <Radio value={TilskuddMottaker.ARRANGOR}>
                    {tilskuddMottakerToString(TilskuddMottaker.ARRANGOR)}
                  </Radio>
                </ControlledRadioGroup>
                {watch("tilskudd")[index].utbetalingMottaker === TilskuddMottaker.ARRANGOR && (
                  <BetalingsinformasjonFields<TilskuddBehandlingRequestTilskuddRequest>
                    arrangorId={arrangorId}
                    kidNummerName={
                      `tilskudd.${index}.kidNummer` as Path<TilskuddBehandlingRequestTilskuddRequest>
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
          onClick={() => append(defaultTilskuddRequest())}
        >
          Legg til tilskudd
        </Button>
        <TotaltBelopBox label="Totalt beløp fra søknad" belop={totaltBelop()} />
      </VStack>
    </>
  );
}
