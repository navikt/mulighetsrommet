import { useTitle } from "@mr/frontend-common";
import { ForsidekortListe } from "./components/forsidekort/ForsidekortListe";
import { BrukerNotifikasjoner } from "./components/notifikasjoner/BrukerNotifikasjoner";
import { HeroBanner } from "./HeroBanner";

export function Forside() {
  useTitle("Nav Tiltaksadministrasjon");
  return (
    <main>
      <div className="bg-[var(--a-deepblue-800)] p-16 min-h-[15rem] w-full mb-6">
        <HeroBanner />
      </div>
      <div className="relative flex flex-col justify-center mx-auto max-w-[1280px] px-4 lg:px-0">
        <BrukerNotifikasjoner />
        <ForsidekortListe />
      </div>
    </main>
  );
}
