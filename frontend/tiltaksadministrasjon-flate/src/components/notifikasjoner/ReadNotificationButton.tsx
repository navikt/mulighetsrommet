import { NotificationStatus } from "@tiltaksadministrasjon/api-client";
import { Button } from "@navikt/ds-react";
import { Dispatch, SetStateAction } from "react";
import { useMutateNotifications } from "@/api/notifikasjoner/useNotifications";

interface Props {
  id: string;
  read: boolean;
  setError: Dispatch<SetStateAction<string>>;
}

export function ReadNotificationButton({ id, read }: Props) {
  const { setNotificationStatus } = useMutateNotifications();

  const setStatus = (status: NotificationStatus) => {
    setNotificationStatus([{ id, status }]);
  };

  return read ? (
    <Button variant="secondary" onClick={() => setStatus(NotificationStatus.UNREAD)} size="small">
      Marker som ulest
    </Button>
  ) : (
    <Button variant="secondary" onClick={() => setStatus(NotificationStatus.READ)} size="small">
      Marker som lest
    </Button>
  );
}
