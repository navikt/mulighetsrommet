import { Heading } from "@navikt/ds-react";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { Notifikasjon } from "./Notifikasjon";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotifications";

export function BrukerNotifikasjoner() {
  const { data: bruker } = useHentAnsatt();
  const { data: summary } = useNotificationSummary();

  if (summary.unreadCount === 0) {
    return null;
  }

  return (
    <div className="mb-4 w-full xl:w-[35%] 2xl:px-0 max-xl:px-4">
      <Heading level="2" size="medium">
        Hei {bruker.fornavn}
      </Heading>
      <Notifikasjon
        href="/oppgaveoversikt/notifikasjoner"
        tittel="Notifikasjoner"
        melding="Du har nye notifikasjoner"
      />
    </div>
  );
}
