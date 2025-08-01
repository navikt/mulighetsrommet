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
    <div className="xl:w-[35%]">
      <Heading level="2" spacing size="medium">
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
