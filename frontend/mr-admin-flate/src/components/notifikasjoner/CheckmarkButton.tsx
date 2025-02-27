import { NotificationStatus } from "@mr/api-client-v2";
import { CheckmarkCircleFillIcon, CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import { Dispatch, SetStateAction } from "react";
import { useFetcher } from "react-router";

interface Props {
  id: string;
  read: boolean;
  setError: Dispatch<SetStateAction<string>>;
}

export function CheckmarkButton({ id, read, setError }: Props) {
  const fetcher = useFetcher();

  const setStatus = async (status: NotificationStatus) => {
    const formData = new FormData();
    formData.set("ids[]", id);
    formData.set("statuses[]", status);
    fetcher.submit(formData, { method: "POST", action: `/arbeidsbenk/notifikasjoner` });
  };

  if (fetcher.data?.error) {
    setError(fetcher.data.error);
  }

  return read ? (
    <Button
      onClick={() => setStatus(NotificationStatus.NOT_DONE)}
      className={`flex items-center justify-center p-2 -mt-2.5 cursor-pointer rounded-lg`}
      size="medium"
    >
      <CheckmarkCircleFillIcon
        fontSize={"2rem"}
        className="p-0 m-0 flex items-center justify-center"
        aria-label="Knapp for å markere notifikasjon som ulest"
      />
    </Button>
  ) : (
    <Button
      onClick={() => setStatus(NotificationStatus.DONE)}
      className={`flex items-center justify-center p-2 -mt-2.5 cursor-pointer`}
      size="medium"
    >
      <CheckmarkCircleIcon
        fontSize={"2rem"}
        className="p-0 m-0 flex items-center justify-center"
        aria-label="Knapp for å markere notifikasjon som lest"
      />
    </Button>
  );
}
