import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Radio, Spacer, TextField, VStack } from "@navikt/ds-react";
import { Path, useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import {
  OpplaeringtilskuddKode,
  TilskuddBehandlingRequest,
  TilskuddBehandlingRequestTilskuddRequest,
  TilskuddMottaker,
  Valuta,
  ValutaBelop,
} from "@tiltaksadministrasjon/api-client";
import { VelgKostnadssted } from "../tilsagn/form/VelgKostnadssted";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { BetalingsinformasjonFields } from "../utbetaling/form/BetalingsinformasjonFields";
import { opplaeringTilskuddToString, tilskuddMottakerToString } from "@/utils/Utils";
import { defaultTilskuddRequest } from "./defaultTilskuddRequest";
import { TotaltBelopBox } from "./TotaltBelopBox";
import { TilskuddFormGroup } from "@/layouts/TilskuddFormGroup";

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
    const tilskudd = watch("tilskudd");
    return {
      belop: tilskudd.reduce((sum, v) => sum + (v.soknadBelop?.belop ?? 0), 0),
      valuta: tilskudd.at(0)?.soknadBelop?.valuta ?? Valuta.NOK,
    };
  }

  return (
    <>
      <Heading size="medium" level="3" spacing>
        Saksopplysninger
      </Heading>
      <VStack gap="space-20">
        {fields.map((field, index) => (
          <TilskuddFormGroup key={field.id}>
            <Heading size="small" level="4" spacing>
              Tilskudd
            </Heading>
            <VStack gap="space-20" align="start">
              <FormTextField
                label="Journalpost-ID i Gosys"
                name={`tilskudd.${index}.soknadJournalpostId`}
                rules={{ required: "Journalpost-ID må fylles ut" }}
              />
              <FormDateInput
                name={`tilskudd.${index}.soknadDato`}
                label="Søknadsdato"
                rules={{ required: "Søknadsdato må fylles ut" }}
              />
              <HStack gap="space-16">
                <FormDateInput
                  name={`tilskudd.${index}.periodeStart`}
                  label="Periodestart"
                  rules={{ required: "Periodestart må fylles ut" }}
                />
                <FormDateInput
                  name={`tilskudd.${index}.periodeSlutt`}
                  label="Periodeslutt"
                  rules={{ required: "Periodeslutt må fylles ut" }}
                />
              </HStack>
              <HStack gap="space-16" align="start">
                <FormSelect
                  label="Tilskuddstype"
                  name={`tilskudd.${index}.tilskuddOpplaeringType`}
                  rules={{ required: "Tilskuddstype må fylles ut" }}
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
                  readOnly
                >
                  <option value={Valuta.NOK}>NOK</option>
                </FormSelect>
                <Spacer />
              </HStack>
              <VelgKostnadssted
                name={`tilskudd.${index}.kostnadssted`}
                kostnadssteder={kostnadssteder.flatMap((r) => r.kostnadssteder.map((k) => k))}
              />
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
              {fields.length > 1 && (
                <Button
                  className="self-end"
                  size="small"
                  variant="secondary"
                  data-color="neutral"
                  icon={<TrashIcon aria-hidden />}
                  onClick={() => remove(index)}
                >
                  Fjern
                </Button>
              )}
            </VStack>
          </TilskuddFormGroup>
        ))}
        <HStack align="start">
          <Button
            size="small"
            type="button"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            onClick={() => append(defaultTilskuddRequest())}
          >
            Legg til tilskudd
          </Button>
        </HStack>
        <TotaltBelopBox label="Totalt beløp fra søknad" belop={totaltBelop()} />
      </VStack>
    </>
  );
}
