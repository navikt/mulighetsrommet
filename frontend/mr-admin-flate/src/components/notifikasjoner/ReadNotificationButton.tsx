import { NotificationStatus } from "@mr/api-client-v2";
import { Button } from "@navikt/ds-react";
import { Dispatch, SetStateAction } from "react";
import { useFetcher } from "react-router";

interface Props {
  id: string;
  read: boolean;
  setError: Dispatch<SetStateAction<string>>;
}

export function ReadNotificationButton({ id, read, setError }: Props) {
  const fetcher = useFetcher();

  const setStatus = async (status: NotificationStatus) => {
    const formData = new FormData();
    formData.set("ids[]", id);
    formData.set("statuses[]", status);
    fetcher.submit(formData, { method: "POST", action: `/oppgaveoversikt/notifikasjoner` });
  };

  if (fetcher.data?.error) {
    setError(fetcher.data.error);
  }

  return read ? (
    <Button variant="secondary" onClick={() => setStatus(NotificationStatus.NOT_DONE)} size="small">
      Marker som ulest
    </Button>
  ) : (
    <Button variant="secondary" onClick={() => setStatus(NotificationStatus.DONE)} size="small">
      Marker som lest
    </Button>
  );
}
