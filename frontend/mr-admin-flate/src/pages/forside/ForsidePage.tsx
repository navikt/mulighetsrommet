import { ForsidekortListe } from "@/pages/forside/forsidekort/ForsidekortListe";
import { BrukerNotifikasjoner } from "@/components/notifikasjoner/BrukerNotifikasjoner";
import { HeroBanner } from "@/pages/forside/HeroBanner";

export function ForsidePage() {
  return (
    <main>
      <title>Nav Tiltaksadministrasjon</title>
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
