import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Heading, VStack, HStack, Radio, TextField } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import { FormGroup } from "@/layouts/FormGroup";
import { TilskuddBehandlingRequest, VedtakResultat } from "@tiltaksadministrasjon/api-client";

export function VedtakForm() {
  const {
    watch,
    register,
    formState: { errors },
  } = useFormContext<TilskuddBehandlingRequest>();

  const vedtak = watch("vedtak");

  return (
    <>
      <Heading size="medium" level="3" spacing>
        Vedtak
      </Heading>
      <VStack gap="space-20" align="start">
        {vedtak.map((v, index) => (
          <FormGroup key={index}>
            <VStack gap="space-4">
              <MetadataVStack label="Tilskuddstype" value={v.tilskuddOpplaeringType} />
              <MetadataVStack label="Hvem skal motta utbetalingen?" value={v.utbetalingMottaker} />
              <MetadataVStack label="Beløp fra søknad" value={v.soknadBelop?.belop} />
            </VStack>
            <Separator />
            <VStack gap="space-8">
              <HStack gap="space-24" align="start" justify="space-between">
                <ControlledRadioGroup
                  size="small"
                  name={`vedtak.${index}.vedtakResultat`}
                  legend="Vedtaksresultat"
                  horisontal
                >
                  <Radio value={VedtakResultat.INNVILGELSE}>Innvilgelse</Radio>
                  <Radio value={VedtakResultat.AVSLAG}>Avslag</Radio>
                </ControlledRadioGroup>
              </HStack>
              {watch("vedtak")[index].vedtakResultat === VedtakResultat.INNVILGELSE && (
                <TextField
                  className="w-[10rem]"
                  size="small"
                  type="text"
                  label="Beløp til utbetaling"
                  error={errors.vedtak?.[index]?.belop?.message}
                  {...register(`vedtak.${index}.belop`, {
                    setValueAs: (v: string) => (v === "" ? null : Number(v)),
                    validate: (value: number | null) => {
                      if (!Number.isInteger(value)) return "Beløp må være et heltall";
                      return true;
                    },
                  })}
                />
              )}
            </VStack>
            <Box width="100%">
              <FormTextarea
                label="Kommentarer til deltaker (vil vises i vedtaksbrev)"
                name={`vedtak.${index}.kommentarVedtaksbrev`}
              />
            </Box>
          </FormGroup>
        ))}
      </VStack>
    </>
  );
}
