import { Heading } from "@navikt/ds-react";
import { BrukerNotifikasjoner } from "./components/notifikasjoner/BrukerNotifikasjoner";
import { ForsidekortListe } from "./components/forsidekort/ForsidekortListe";
import { useTitle } from "@mr/frontend-common";

export function Forside() {
  useTitle("Nav Tiltaksadministrasjon");
  return (
    <main>
      <div className="bg-[var(--a-deepblue-800)] p-16 min-h-[15rem] w-full mb-6">
        <Heading size="large" level="2" className="text-white p-0 m-0" data-testid="heading">
          Enkel og effektiv administrasjon
          <br /> av arbeidsmarkedstiltak
        </Heading>
      </div>
      <div className="relative flex flex-col justify-center mx-auto max-w-[1280px] px-4 lg:px-0">
        <BrukerNotifikasjoner />
        <ForsidekortListe />
      </div>
    </main>
  );
}
