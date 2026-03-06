import { UthevetBox } from "@/layouts/UthevetBox";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Heading, VStack, TextField, HStack, Radio } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import type { BehandlingFormData } from "./schema";

export function Vilkarsvurdering() {
  const { watch } = useFormContext<BehandlingFormData>();

  const tilskudd = watch("tilskudd");
  const totalBelop = tilskudd.reduce((sum, t) => {
    const belop = parseInt(t.belopTilUtbetaling || t.belop || "0");
    return sum + (isNaN(belop) ? 0 : belop);
  }, 0);

  return (
    <>
      <Heading size="medium" level="3" spacing>
        Vilkårsvurdering av tilskudd
      </Heading>
      <VStack gap="space-20">
        {tilskudd.map((tilskuddItem, index) => (
          <UthevetBox key={index}>
            <HStack gap="space-24">
              <MetadataVStack
                label="Tilskuddstype"
                value={tilskuddItem.tilskuddstype || "Ikke valgt"}
              />
              <MetadataVStack label="Innsendt beløp" value={`${tilskuddItem.belop || 0} NOK`} />
            </HStack>
            <FormTextField
              label="Beløp til utbetaling"
              name={`tilskudd.${index}.belopTilUtbetaling`}
            />
            <ControlledRadioGroup
              name={`tilskudd.${index}.nodvendigForOpplaring`}
              legend="Er tilskuddet nødvendig for opplæring?"
              size="small"
              horisontal
              rules={{ required: "Velg Ja eller Nei" }}
            >
              <Radio value="yes">Ja</Radio>
              <Radio value="no">Nei</Radio>
            </ControlledRadioGroup>
            <Box width="100%">
              <FormTextarea label="Begrunnelse" name={`tilskudd.${index}.begrunnelse`} />
            </Box>
          </UthevetBox>
        ))}
        <Heading size="small" level="4">
          Maksbeløp
        </Heading>
        <TextField
          label="Totalt beløp til utbetaling"
          size="small"
          value={totalBelop || "0"}
          readOnly
        />
        <ControlledRadioGroup
          name="belopInnenforMaksgrense"
          legend="Er beløpet innenfor maksgrense for utdanningsår og utdanningsløp?"
          description="Krever en forklaring her om det man må vurdere osv"
          size="small"
          horisontal
          rules={{ required: "Velg Ja eller Nei" }}
        >
          <Radio value="yes">Ja</Radio>
          <Radio value="no">Nei</Radio>
        </ControlledRadioGroup>
        <ControlledRadioGroup
          name="unntakVurdert"
          legend="Er det vurdert unntak fra maksgrensen?"
          description="Krever en forklaring her om det man må vurdere osv"
          size="small"
          horisontal
          rules={{ required: "Velg Ja eller Nei" }}
        >
          <Radio value="yes">Ja</Radio>
          <Radio value="no">Nei</Radio>
        </ControlledRadioGroup>
        <Box width="100%">
          <FormTextarea label="Begrunnelse" name="maksbelopBegrunnelse" />
        </Box>
      </VStack>
    </>
  );
}
