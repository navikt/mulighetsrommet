import { Heading } from "@navikt/ds-react";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotificationSummary";
import { Notifikasjon } from "./Notifikasjon";

export function BrukerNotifikasjoner() {
  const { data: bruker } = useHentAnsatt();
  const { data: notificationSummary } = useNotificationSummary();
  const antallUlesteNotifikasjoner = notificationSummary?.notDoneCount || -1;

  if (antallUlesteNotifikasjoner <= 0) return null;

  return (
    <div className="mb-4 w-full xl:w-[35%] 2xl:px-0 max-xl:px-4">
      <Heading level="2" size="medium">
        Hei {bruker?.fornavn}
      </Heading>
      <Notifikasjon
        href="/notifikasjoner"
        tittel="Notifikasjoner"
        melding="Du har nye notifikasjoner"
      />
    </div>
  );
}
