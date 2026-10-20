import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HGrid, HStack, Radio, Spacer, TextField, VStack } from "@navikt/ds-react";
import { Path, useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import {
  GjennomforingEnkeltplassDtoAnsvarligEnhet,
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
import z from "zod";

interface Props {
  arrangorId: string;
  ansvarligEnhet: GjennomforingEnkeltplassDtoAnsvarligEnhet;
}

export function SaksopplysningerForm({ arrangorId, ansvarligEnhet }: Props) {
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

  const sisteDatoIAar = new Date(new Date().getFullYear(), 11, 31);

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
            <VStack gap="space-24">
              <HGrid columns="0.8fr 1fr" gap="space-24" align="start">
                <FormTextField
                  label="Journalpost-ID i Gosys"
                  name={`tilskudd.${index}.soknadJournalpostId`}
                  autoFocus
                  rules={{ required: "Journalpost-ID er påkrevd" }}
                />
                <FormDateInput
                  name={`tilskudd.${index}.soknadDato`}
                  label="Søknadsdato"
                  rules={{ required: "Søknadsdato er påkrevd" }}
                />
                <FormDateInput
                  name={`tilskudd.${index}.periodeStart`}
                  label="Periodestart"
                  rules={{ required: "Periodestart er påkrevd" }}
                  toDate={sisteDatoIAar}
                  invalidDatoEtterPeriode="Periodestart kan ikke være etter siste dag i inneværende år"
                />
                <FormDateInput
                  name={`tilskudd.${index}.periodeSlutt`}
                  label="Periodeslutt"
                  rules={{ required: "Periodeslutt er påkrevd" }}
                  toDate={sisteDatoIAar}
                  invalidDatoEtterPeriode="Periodeslutt kan ikke være etter siste dag i inneværende år"
                />
                <VelgKostnadssted
                  name={`tilskudd.${index}.kostnadssted`}
                  kostnadssteder={kostnadssteder.flatMap((r) => r.kostnadssteder.map((k) => k))}
                />
                <Spacer />
                <FormSelect
                  label="Tilskuddstype"
                  name={`tilskudd.${index}.tilskuddOpplaeringType`}
                  rules={{ required: "Tilskuddstype er påkrevd" }}
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
                <HStack gap="space-16" align="start">
                  <TextField
                    size="small"
                    type="text"
                    label="Beløp fra faktura"
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
                </HStack>
              </HGrid>
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
            onClick={() =>
              append(defaultTilskuddRequest(ansvarligEnhet.enhetsnummer), {
                focusName: `tilskudd.${fields.length}.soknadJournalpostId`,
              })
            }
          >
            Legg til tilskudd
          </Button>
        </HStack>
        <TotaltBelopBox label="Totalt beløp fra fakturaer" belop={totaltBelop()} />
      </VStack>
    </>
  );
}

export const saksopplysningerSchema = z
  .object({
    tilskudd: z
      .array(
        z
          .object({
            soknadJournalpostId: z
              .string({ error: "Journalpost-ID er påkrevd" })
              .min(1, "Journalpost-ID er påkrevd"),
            soknadDato: z
              .string({ error: "Søknadsdato er påkrevd" })
              .min(1, "Søknadsdato er påkrevd"),
            periodeStart: z
              .string({ error: "Periodestart er påkrevd" })
              .min(1, "Periodestart er påkrevd"),
            periodeSlutt: z
              .string({ error: "Periodeslutt er påkrevd" })
              .min(1, "Periodeslutt er påkrevd"),
            kostnadssted: z
              .string({ error: "Kostnadssted er påkrevd" })
              .min(1, "Kostnadssted er påkrevd"),
            tilskuddOpplaeringType: z.enum(OpplaeringtilskuddKode, {
              error: "Tilskuddstype er påkrevd",
            }),
            soknadBelop: z
              .object({
                belop: z
                  .number({ error: "Beløp må være et heltall" })
                  .int("Beløp må være et heltall"),
              })
              .loose(),
            utbetalingMottaker: z.enum(TilskuddMottaker, {
              error: "Mottaker må være satt",
            }),
          })
          .loose(),
      )
      .min(1, "Minst ett tilskudd er påkrevd"),
  })
  .loose();
