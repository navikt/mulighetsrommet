import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { NotificationStatus } from "@tiltaksadministrasjon/api-client";
import { Button, HStack } from "@navikt/ds-react";
import { EmptyState } from "./EmptyState";
import { NotifikasjonerListItem } from "./NotifikasjonerListItem";
import { useMutateNotifications, useNotifications } from "@/api/notifikasjoner/useNotifications";

interface Props {
  lest: boolean;
}

export function NotifikasjonerList({ lest }: Props) {
  const {
    data: { data: notifikasjoner },
  } = useNotifications(lest ? NotificationStatus.READ : NotificationStatus.UNREAD);

  const { setNotificationStatus } = useMutateNotifications();

  function toggleMarkertSomlestUlest() {
    if (notifikasjoner.length === 0) {
      return;
    }

    const newStatus = lest ? NotificationStatus.UNREAD : NotificationStatus.READ;

    const updatedNotifikasjoner = notifikasjoner.map(({ id }) => ({
      id,
      status: newStatus,
    }));

    setNotificationStatus(updatedNotifikasjoner);
  }

  if (notifikasjoner.length === 0) {
    return (
      <EmptyState
        tittel={lest ? "Du har ingen tidligere notifikasjoner" : "Ingen nye notifikasjoner"}
        beskrivelse={
          lest
            ? "Når du har gjort en oppgave eller lest en beskjed havner de her"
            : "Vi varsler deg når noe skjer"
        }
      />
    );
  }

  return (
    <ReloadAppErrorBoundary>
      <div className="max-w-[1440px] mt-5">
        <HStack align={"end"} justify={"end"}>
          <Button variant="tertiary-neutral" size="small" onClick={toggleMarkertSomlestUlest}>
            Merk alle som {lest ? "ulest" : "lest"}
          </Button>
        </HStack>
        <ul className="m-0 mb-4 pl-0 flex flex-col">
          {notifikasjoner.map((n) => {
            return <NotifikasjonerListItem lest={lest} key={n.id} notifikasjon={n} />;
          })}
        </ul>
      </div>
    </ReloadAppErrorBoundary>
  );
}
