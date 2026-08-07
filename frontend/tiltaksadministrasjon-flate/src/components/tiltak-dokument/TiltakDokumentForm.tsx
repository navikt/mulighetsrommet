import { Tabs, VStack } from "@navikt/ds-react";
import { TiltakDokumentDetaljerForm } from "./TiltakDokumentDetaljerForm";
import { TiltakDokumentVeilederinformasjonForm } from "./TiltakDokumentVeilederinformasjonForm";

export function TiltakDokumentForm() {
  return (
    <Tabs defaultValue="detaljer">
      <Tabs.List>
        <Tabs.Tab value="detaljer" label="Detaljer" />
        <Tabs.Tab value="veilederinformasjon" label="Informasjon for veiledere" />
      </Tabs.List>

      <Tabs.Panel value="detaljer">
        <VStack paddingBlock="space-16 space-0">
          <TiltakDokumentDetaljerForm />
        </VStack>
      </Tabs.Panel>

      <Tabs.Panel value="veilederinformasjon">
        <VStack paddingBlock="space-16 space-0">
          <TiltakDokumentVeilederinformasjonForm />
        </VStack>
      </Tabs.Panel>
    </Tabs>
  );
}
