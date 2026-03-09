import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Heading, VStack, TextField, HStack, Radio, Spacer } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import type { BehandlingFormData } from "./schema";
import { FormGroup } from "@/layouts/FormGroup";

export function Vedtak() {
  const { watch } = useFormContext<BehandlingFormData>();

  const tilskudd = watch("tilskudd");
  const totalBelop = tilskudd.reduce((sum, t) => {
    const belop = parseInt(t.belopTilUtbetaling || t.belop || "0");
    return sum + (isNaN(belop) ? 0 : belop);
  }, 0);

  return (
    <>
      <Heading size="medium" level="3" spacing>
        Vedtak
      </Heading>
      <VStack gap="space-20" align="start">
        {tilskudd.map((tilskuddItem, index) => (
          <FormGroup key={index}>
            <HStack gap="space-24" align="start" justify="space-between">
              <MetadataVStack
                label="Tilskuddstype"
                value={tilskuddItem.tilskuddstype || "Ikke valgt"}
              />
              <MetadataVStack
                label="Beløp til utbetaling"
                value={`${tilskuddItem.belopTilUtbetaling || tilskuddItem.belop || 0} NOK`}
              />
              <Spacer />
              <ControlledRadioGroup
                name={`tilskudd.${index}.vedtaksresultat`}
                legend="Vedtaksresultat"
                horisontal
                rules={{ required: "Velg vedtaksresultat" }}
              >
                <Radio value="innvilgelse">Innvilgelse</Radio>
                <Radio value="avslag">Avslag</Radio>
              </ControlledRadioGroup>
            </HStack>
          </FormGroup>
        ))}
        <TextField
          label="Totalt beløp til utbetaling"
          size="small"
          value={totalBelop || "0"}
          readOnly
        />
        <ControlledRadioGroup
          name="mottakerAvUtbetaling"
          legend="Hvem skal motta utbetalingen?"
          size="small"
          horisontal
          rules={{ required: "Velg mottaker" }}
        >
          <Radio value="deltaker">Deltaker</Radio>
          <Radio value="arrangor">Arrangør</Radio>
        </ControlledRadioGroup>
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
