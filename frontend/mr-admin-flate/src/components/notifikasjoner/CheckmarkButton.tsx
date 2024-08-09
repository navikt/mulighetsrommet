import { Dispatch, SetStateAction } from "react";
import { CheckmarkCircleFillIcon, CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { NotificationStatus } from "mulighetsrommet-api-client";
import classNames from "classnames";
import styles from "./CheckmarkButton.module.scss";
import { useSetNotificationStatus } from "@/api/notifikasjoner/useSetNotificationStatus";
import { Button } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../../api/QueryKeys";

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
      className={classNames(styles.button, styles.read)}
      size="medium"
    >
      <CheckmarkCircleFillIcon
        fontSize={"2rem"}
        className={styles.icon}
        aria-label="Knapp for å markere notifikasjon som ulest"
      />
    </Button>
  ) : (
    <Button
      onClick={() => setStatus(NotificationStatus.DONE)}
      className={classNames(styles.button, styles.unread)}
      size="medium"
    >
      <CheckmarkCircleIcon
        fontSize={"2rem"}
        className={styles.icon}
        aria-label="Knapp for å markere notifikasjon som lest"
      />
    </Button>
  );
}
