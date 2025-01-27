import { Dispatch, SetStateAction } from "react";
import { CheckmarkCircleFillIcon, CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { NotificationStatus } from "@mr/api-client-v2";
import { useSetNotificationStatus } from "@/api/notifikasjoner/useSetNotificationStatus";
import { Button } from "@navikt/ds-react";
import { QueryKeys } from "../../api/QueryKeys";
import { useQueryClient } from "@tanstack/react-query";

interface Props {
  id: string;
  read: boolean;
  setRead: Dispatch<SetStateAction<boolean>>;
  setError: Dispatch<SetStateAction<string>>;
}

export function CheckmarkButton({ id, read, setRead, setError }: Props) {
  const { mutate } = useSetNotificationStatus(id);
  const queryClient = useQueryClient();

  const setStatus = async (status: NotificationStatus) => {
    mutate(
      { status },
      {
        onSuccess: () => {
          setRead(status === NotificationStatus.DONE);
          queryClient.invalidateQueries({
            queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.NOT_DONE),
          });
          queryClient.invalidateQueries({
            queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.DONE),
          });
        },
        onError: () => {
          setError("Klarte ikke lagre endring");
        },
      },
    );
  };

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
