import { ForsidekortListe } from "@/pages/forside/forsidekort/ForsidekortListe";
import { BrukerNotifikasjoner } from "@/components/notifikasjoner/BrukerNotifikasjoner";
import { HeroBanner } from "@/pages/forside/HeroBanner";
import { VStack } from "@navikt/ds-react";

export function ForsidePage() {
  return (
    <>
      <title>Nav Tiltaksadministrasjon</title>
      <HeroBanner />
      <VStack gap="4" justify="center" className="mx-auto lg:w-5xl max-w-[1280px] px-4">
        <BrukerNotifikasjoner />
        <ForsidekortListe />
      </VStack>
    </>
  );
}
