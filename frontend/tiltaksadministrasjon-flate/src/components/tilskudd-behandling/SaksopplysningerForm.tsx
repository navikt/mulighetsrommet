import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import {
  Bleed,
  BodyShort,
  Button,
  Heading,
  HGrid,
  HStack,
  Radio,
  Spacer,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { Path, useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import {
  BrukerUtbetalingType,
  AnsvarligEnhetDto,
  OpplaeringtilskuddKode,
  TilskuddBehandlingRequest,
  TilskuddVedtakRequest,
  TilskuddMottaker,
  Valuta,
  ValutaBelop,
} from "@tiltaksadministrasjon/api-client";
import { VelgKostnadssted } from "../tilsagn/form/VelgKostnadssted";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { BetalingsinformasjonFields } from "../utbetaling/form/BetalingsinformasjonFields";
import { opplaeringTilskuddToString } from "@/utils/Utils";
import { defaultTilskuddRequest } from "./defaultTilskuddRequest";
import { TotaltBelopBox } from "./TotaltBelopBox";
import { TilskuddFormGroup } from "@/layouts/TilskuddFormGroup";

interface Props {
  arrangorId: string;
  ansvarligEnhet: AnsvarligEnhetDto;
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
    name: "tilskuddVedtak",
  });

  const sisteDatoIAar = new Date(new Date().getFullYear(), 11, 31);

  function totaltBelop(): ValutaBelop {
    const tilskuddVedtak = watch("tilskuddVedtak");
    return {
      belop: tilskuddVedtak.reduce((sum, v) => sum + (v.soknadBelop?.belop ?? 0), 0),
      valuta: tilskuddVedtak.at(0)?.soknadBelop?.valuta ?? Valuta.NOK,
    };
  }

  const tilskuddstype = Object.keys(OpplaeringtilskuddKode) as OpplaeringtilskuddKode[];
  tilskuddstype.sort((a: OpplaeringtilskuddKode, b: OpplaeringtilskuddKode) => a.localeCompare(b));

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
                  name={`tilskuddVedtak.${index}.soknadJournalpostId`}
                  autoFocus
                  rules={{ required: "Journalpost-ID er påkrevd" }}
                />
                <FormDateInput
                  name={`tilskuddVedtak.${index}.soknadDato`}
                  label="Søknadsdato"
                  rules={{ required: "Søknadsdato er påkrevd" }}
                />
                <FormDateInput
                  name={`tilskuddVedtak.${index}.periodeStart`}
                  label="Periodestart"
                  rules={{ required: "Periodestart er påkrevd" }}
                  toDate={sisteDatoIAar}
                  invalidDatoEtterPeriode="Periodestart kan ikke være etter siste dag i inneværende år"
                />
                <FormDateInput
                  name={`tilskuddVedtak.${index}.periodeSlutt`}
                  label="Periodeslutt"
                  rules={{ required: "Periodeslutt er påkrevd" }}
                  toDate={sisteDatoIAar}
                  invalidDatoEtterPeriode="Periodeslutt kan ikke være etter siste dag i inneværende år"
                />
                <VelgKostnadssted name={`tilskuddVedtak.${index}.kostnadssted`} />
                <Spacer />
                <FormSelect
                  label="Tilskuddstype"
                  name={`tilskuddVedtak.${index}.tilskuddOpplaeringType`}
                  rules={{ required: "Tilskuddstype er påkrevd" }}
                >
                  <option value="">-- Velg tilskuddstype --</option>
                  {tilskuddstype.map((tilskudd) => (
                    <option key={tilskudd} value={tilskudd}>
                      {opplaeringTilskuddToString(tilskudd)}
                    </option>
                  ))}
                </FormSelect>
                <HStack gap="space-16" align="start">
                  <TextField
                    size="small"
                    type="text"
                    label="Beløp fra faktura"
                    error={errors.tilskuddVedtak?.[index]?.soknadBelop?.belop?.message}
                    {...register(`tilskuddVedtak.${index}.soknadBelop.belop`, {
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
                    name={`tilskuddVedtak.${index}.soknadBelop.valuta`}
                    readOnly
                  >
                    <option value={Valuta.NOK}>NOK</option>
                  </FormSelect>
                </HStack>
              </HGrid>
              <MottakerValg index={index} arrangorId={arrangorId} />
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
                focusName: `tilskuddVedtak.${fields.length}.soknadJournalpostId`,
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

interface MottakerValgProps {
  index: number;
  arrangorId: string;
}

function MottakerValg({ index, arrangorId }: MottakerValgProps) {
  const { watch } = useFormContext<TilskuddBehandlingRequest>();
  const tilskuddVedtak = watch("tilskuddVedtak")[index];

  return (
    <>
      <ControlledRadioGroup
        size="small"
        name={`tilskuddVedtak.${index}.utbetalingMottaker`}
        legend="Hvem skal motta utbetalingen?"
        horisontal
      >
        <Radio value={TilskuddMottaker.BRUKER}>Utbetales til brukeren</Radio>
        <Radio value={TilskuddMottaker.ARRANGOR}>Utbetales til arrangøren</Radio>
      </ControlledRadioGroup>
      {tilskuddVedtak.utbetalingMottaker === TilskuddMottaker.BRUKER && (
        <>
          <ControlledRadioGroup
            size="small"
            name={`tilskuddVedtak.${index}.brukerUtbetalingType`}
            legend="Har bruker lagt ut for tilskuddet selv?"
            horisontal
          >
            <Radio value={BrukerUtbetalingType.REFUSJON}>
              Ja, bruker har betalt til utdanningsinstitusjonen
            </Radio>
            <Radio value={BrukerUtbetalingType.FORSKUDD}>
              Nei, det skal utbetales som forskudd
            </Radio>
          </ControlledRadioGroup>
          <Bleed marginBlock="space-16 space-0">
            <BodyShort size="small">
              Husk at det skal være avklart med Nav-veileder at bruker skal få utbetalt som forskudd
            </BodyShort>
          </Bleed>
        </>
      )}{" "}
      {tilskuddVedtak.utbetalingMottaker === TilskuddMottaker.ARRANGOR && (
        <>
          <Bleed marginBlock="space-16 space-0">
            <BodyShort size="small">
              Husk at arrangør må ha sendt faktura med Nav som mottaker av fakturaen
            </BodyShort>
          </Bleed>
          <BetalingsinformasjonFields<TilskuddVedtakRequest>
            arrangorId={arrangorId}
            kidNummerName={`tilskuddVedtak.${index}.kidNummer` as Path<TilskuddVedtakRequest>}
          />
        </>
      )}
    </>
  );
}
