import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Heading, VStack, HStack, Radio, TextField, Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import { FormGroup } from "@/layouts/FormGroup";
import {
  TilskuddBehandlingRequest,
  Valuta,
  VedtakResultat,
} from "@tiltaksadministrasjon/api-client";
import { opplaeringTilskuddToString, tilskuddMottakerToString } from "@/utils/Utils";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { TotaltBelopBox } from "./TotaltBelopBox";

export function VedtakForm() {
  const {
    watch,
    register,
    formState: { errors },
  } = useFormContext<TilskuddBehandlingRequest>();

  const tilskudd = watch("tilskudd");

  return (
    <>
      <VStack gap="space-20">
        <VStack gap="space-8">
          <Definisjonsliste
            definitions={[
              { key: "Journalpost-ID", value: watch("soknadJournalpostId") },
              { key: "Søknadsdato", value: formaterDato(watch("soknadDato")) },
            ]}
          />
          <Separator />
          <Heading size="small" level="3" spacing>
            Søknadsperiode
          </Heading>
          <Definisjonsliste
            definitions={[
              { key: "Periodestart", value: formaterDato(watch("periodeStart")) },
              { key: "Periodeslutt", value: formaterDato(watch("periodeSlutt")) },
              { key: "Kostnadssted", value: watch("kostnadssted") },
            ]}
          />
        </VStack>
        {tilskudd.map((t, index) => (
          <FormGroup key={index}>
            <VStack gap="space-4">
              <MetadataVStack
                label="Tilskuddstype"
                value={
                  t.tilskuddOpplaeringType
                    ? opplaeringTilskuddToString(t.tilskuddOpplaeringType)
                    : "-"
                }
              />
              <MetadataVStack
                label="Hvem skal motta utbetalingen?"
                value={t.utbetalingMottaker ? tilskuddMottakerToString(t.utbetalingMottaker) : "-"}
              />
              <MetadataVStack
                label="Beløp fra søknad"
                value={formaterValuta(
                  t.soknadBelop?.belop ?? 0,
                  t.soknadBelop?.valuta ?? Valuta.NOK,
                )}
              />
            </VStack>
            <Separator />
            <VStack gap="space-8">
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
                    className="w-[10rem]"
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
                    <option value={Valuta.SEK}>SEK</option>
                  </Select>
                </HStack>
              )}
            </VStack>
            <Box width="100%">
              <FormTextarea
                label="Kommentarer til deltaker (vil vises i vedtaksbrev)"
                name={`tilskudd.${index}.kommentarVedtaksbrev`}
              />
            </Box>
          </FormGroup>
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
        <FormTextarea className="w-full" label="Kommentar (internt i Nav)" name="kommentarIntern" />
      </VStack>
    </>
  );
}
