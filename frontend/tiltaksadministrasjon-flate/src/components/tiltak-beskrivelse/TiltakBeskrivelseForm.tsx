import { Tabs, VStack } from "@navikt/ds-react";
import { TiltakBeskrivelseDetaljerForm } from "./TiltakBeskrivelseDetaljerForm";
import { TiltakBeskrivelseVeilederinformasjonForm } from "./TiltakBeskrivelseVeilederinformasjonForm";

interface Props {
  lagredeKontaktpersoner?: { navIdent: string; navn: string }[];
}

export function TiltakBeskrivelseForm({ lagredeKontaktpersoner }: Props) {
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
          <TiltakBeskrivelseVeilederinformasjonForm
            lagredeKontaktpersoner={lagredeKontaktpersoner}
          />
        </VStack>
      </Tabs.Panel>
    </Tabs>
  );
}
