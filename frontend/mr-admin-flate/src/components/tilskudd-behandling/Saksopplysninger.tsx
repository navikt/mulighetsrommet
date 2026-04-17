import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Radio, Spacer, VStack } from "@navikt/ds-react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormGroup } from "@/layouts/FormGroup";
import {
  TilskuddBehandlingRequest,
  TilskuddBehandlingRequestTilskuddVedtakRequest,
  TilskuddOpplaeringType,
  Valuta,
  VedtakResultat,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";
import { KostnadsstedOption, VelgKostnadssted } from "../tilsagn/form/VelgKostnadssted";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";

const tomtTilskudd: TilskuddBehandlingRequestTilskuddVedtakRequest = {
  id: v4(),
  tilskuddOpplaeringType: TilskuddOpplaeringType.SKOLEPENGER,
  soknadBelop: {
    belop: null,
    valuta: Valuta.NOK,
  },
  kommentarVedtaksbrev: null,
  vedtakResultat: VedtakResultat.INNVILGELSE,
  utbetalingMottaker: "bruker",
};

export function SaksopplysningerForm() {
  const { control, watch } = useFormContext<TilskuddBehandlingRequest>();

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
                  <FormSelect
                    label="Tilskuddstype"
                    name={`vedtak.${index}.tilskuddType`}
                    rules={{ required: "Tilskuddstype er påkrevd" }}
                  >
                    <option value="">-- Velg tilskuddstype --</option>
                    {Object.keys(TilskuddOpplaeringType).map((tilskudd) => (
                      <option key={tilskudd} value={tilskudd}>
                        {tilskudd}
                      </option>
                    ))}
                  </FormSelect>
                  <FormTextField
                    label="Beløp fra søknad"
                    name={`vedtak.${index}.soknadBelop.belop`}
                    rules={{ required: "Beløp er påkrevd" }}
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
          onClick={() => append(tomtTilskudd)}
        >
          Legg til tilskudd
        </Button>
      </VStack>
    </>
  );
}
