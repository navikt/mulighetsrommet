import { Tabs, VStack } from "@navikt/ds-react";
import { TiltakBeskrivelseDetaljerForm } from "./TiltakBeskrivelseDetaljerForm";
import { TiltakBeskrivelseVeilederinformasjonForm } from "./TiltakBeskrivelseVeilederinformasjonForm";

export function TiltakBeskrivelseForm() {
  return (
    <Tabs defaultValue="detaljer">
      <Tabs.List>
        <Tabs.Tab value="detaljer" label="Detaljer" />
        <Tabs.Tab value="veilederinformasjon" label="Informasjon for veiledere" />
      </Tabs.List>

      <Tabs.Panel value="detaljer">
        <VStack paddingBlock="space-16 space-0">
          <TiltakBeskrivelseDetaljerForm />
        </VStack>
      </Tabs.Panel>

      <Tabs.Panel value="veilederinformasjon">
        <VStack paddingBlock="space-16 space-0">
          <TiltakBeskrivelseVeilederinformasjonForm />
        </VStack>
      </Tabs.Panel>
    </Tabs>
  );
}
