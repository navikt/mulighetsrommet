import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Heading, VStack, HStack, Radio } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import { FormGroup } from "@/layouts/FormGroup";
import { TilskuddBehandlingRequest } from "@tiltaksadministrasjon/api-client";

export function VedtakForm() {
  const { watch } = useFormContext<TilskuddBehandlingRequest>();

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
            <HStack gap="space-24" align="start" justify="space-between">
              <ControlledRadioGroup
                size="small"
                name={`vedtak.${index}.vedtaksresultat`}
                legend="Vedtaksresultat"
                horisontal
              >
                <Radio value="innvilgelse">Innvilgelse</Radio>
                <Radio value="avslag">Avslag</Radio>
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
