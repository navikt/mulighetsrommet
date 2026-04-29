import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Heading, VStack, HStack, Radio } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import { FormGroup } from "@/layouts/FormGroup";
import { TilskuddBehandlingRequest, VedtakResultat } from "@tiltaksadministrasjon/api-client";
import { opplaeringTilskuddToString } from "@/utils/Utils";

export function VedtakForm() {
  const { watch } = useFormContext<TilskuddBehandlingRequest>();

  const tilskudd = watch("tilskudd");

  return (
    <>
      <Heading size="medium" level="3" spacing>
        Vedtak
      </Heading>
      <VStack gap="space-20" align="start">
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
              <MetadataVStack label="Hvem skal motta utbetalingen?" value={t.utbetalingMottaker} />
              <MetadataVStack label="Beløp fra søknad" value={t.soknadBelop?.belop} />
            </VStack>
            <Separator />
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
          </FormGroup>
        ))}
        <Box width="100%">
          <FormTextarea
            label="Kommentarer til deltaker (vil vises i vedtaksbrev)"
            name="kommentarerTilDeltaker"
          />
        </Box>
      </VStack>
    </>
  );
}
