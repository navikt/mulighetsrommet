import { Dispatch, SetStateAction } from "react";
import { CheckmarkCircleFillIcon, CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { NotificationStatus } from "mulighetsrommet-api-client";
import classNames from "classnames";
import styles from "./CheckmarkButton.module.scss";
import { useSetNotificationStatus } from "@/api/notifikasjoner/useSetNotificationStatus";
import { Button } from "@navikt/ds-react";
import { toast } from "react-toastify";

interface Props {
  id: string;
  read: boolean;
  setRead: Dispatch<SetStateAction<boolean>>;
}

export function CheckmarkButton({ id, read, setRead }: Props) {
  const { mutate } = useSetNotificationStatus(id);

  const setStatus = async (status: NotificationStatus) => {
    mutate(
      { status },
      {
        onSuccess: () => {
          setRead(status === NotificationStatus.DONE);
          toast.success(
            `Notifikasjon markert som ${status === NotificationStatus.DONE ? "lest" : "ulest"}`,
            {
              hideProgressBar: true,
              autoClose: 1000,
            },
          );
        },
        onError: () => {
          toast.error("Klarte ikke oppdatere notifikasjon");
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
