import { Heading, HStack, Radio, Select, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import {
  TilskuddBehandlingRequest,
  Valuta,
  VedtakResultat,
} from "@tiltaksadministrasjon/api-client";
import { addDuration, yyyyMMddSafeFormatting } from "@mr/frontend-common/utils/date";
import { TotaltBelopBox } from "./TotaltBelopBox";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { NyFormGroup } from "@/layouts/NyFormGroup";
import { SaksOpplysninger } from "./Saksopplysninger";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

export function VedtakForm() {
  const {
    watch,
    register,
    formState: { errors },
  } = useFormContext<TilskuddBehandlingRequest>();

  const { data: kostnadssteder } = useKostnadssteder();

  const tilskudd = watch("tilskudd");

  // FIXME: Småhacky konvertering fra separate dato-felter til Periode
  //  Det ideelle hadde kanskje heller vært å ta høyde off-by-one-problematikken ved å ha en egen FormPeriode-komponent
  const valgtPeriode = (start: string | null, slutt: string | null) =>
    start && slutt
      ? {
          start,
          slutt: yyyyMMddSafeFormatting(addDuration(new Date(slutt), { days: 1 })),
        }
      : null;
  const valgtKostnadsted = (kostnadsted: string | null) =>
    kostnadssteder
      .flatMap((region) => region.kostnadssteder)
      .find((k) => k.enhetsnummer === kostnadsted) || null;
  return (
    <>
      <Heading size="medium" level="3" spacing>
        Vedtak og beregning
      </Heading>
      <VStack gap="space-32">
        {tilskudd.map((t, index) => (
          <NyFormGroup key={index}>
            <SaksOpplysninger
              journalpostId={t.soknadJournalpostId}
              soknadsdato={t.soknadDato}
              periode={valgtPeriode(t.periodeStart, t.periodeSlutt)}
              kostnadssted={valgtKostnadsted(t.kostnadssted)}
              belop={t.soknadBelop?.belop ?? 0}
              utbetalingMottaker={t.utbetalingMottaker}
              tilskuddOpplaeringType={t.tilskuddOpplaeringType}
            />
            <Separator />
            <Heading size="small" level="3" spacing>
              Vedtak
            </Heading>
            <VStack gap="space-20">
              <HStack gap="space-24" align="start" justify="space-between">
                <ControlledRadioGroup
                  size="small"
                  name={`tilskudd.${index}.vedtakResultat`}
                  legend="Vedtaksresultat"
                  horisontal
                >
                  <Radio value={VedtakResultat.INNVILGELSE}>Innvilgelse</Radio>
                  <Radio value={VedtakResultat.AVSLAG}>Avslag</Radio>
                </ControlledRadioGroup>
              </HStack>
              {watch("tilskudd")[index].vedtakResultat === VedtakResultat.INNVILGELSE && (
                <HStack align="start" gap="space-8">
                  <TextField
                    className="w-40"
                    size="small"
                    type="text"
                    label="Beløp til utbetaling"
                    error={errors.tilskudd?.[index]?.belop?.message}
                    {...register(`tilskudd.${index}.belop`, {
                      setValueAs: (v: string) => (v === "" ? null : Number(v)),
                      validate: (value: number | null) => {
                        if (!Number.isInteger(value)) return "Beløp må være et heltall";
                        return true;
                      },
                    })}
                  />
                  <Select size="small" readOnly value={Valuta.NOK} label="Valuta">
                    <option value={Valuta.NOK}>NOK</option>
                  </Select>
                </HStack>
              )}
              <FormTextarea
                label="Kommentar til deltaker (vil vises i vedtaksbrev)"
                name={`tilskudd.${index}.kommentarVedtaksbrev`}
              />
              <FormTextarea
                label="Kommentar (internt i Nav)"
                name={`tilskudd.${index}.kommentarIntern`}
              />
            </VStack>
          </NyFormGroup>
        ))}
        <TotaltBelopBox
          label="Totalt beløp fra søknad"
          belop={{
            belop: watch("tilskudd").reduce((sum, t) => sum + (t.soknadBelop?.belop ?? 0), 0),
            valuta: watch("tilskudd").at(0)?.soknadBelop?.valuta ?? Valuta.NOK,
          }}
        />
        <TotaltBelopBox
          label="Totalt beløp til utbetaling"
          belop={{
            belop: watch("tilskudd").reduce((sum, t) => sum + (t.belop ?? 0), 0),
            valuta: Valuta.NOK,
          }}
        />
      </VStack>
    </>
  );
}
