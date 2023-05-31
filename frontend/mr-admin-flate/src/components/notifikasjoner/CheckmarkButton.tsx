import { Dispatch, SetStateAction } from "react";
import {
  CheckmarkCircleFillIcon,
  CheckmarkCircleIcon,
} from "@navikt/aksel-icons";
import { NotificationStatus } from "mulighetsrommet-api-client";
import classNames from "classnames";
import styles from "./CheckmarkButton.module.scss";
import { useSetNotificationStatus } from "../../api/notifikasjoner/useSetNotificationStatus";
import { Button } from "@navikt/ds-react";

interface Props {
  id: string;
  read: boolean;
  setRead: Dispatch<SetStateAction<boolean>>;
}

export function CheckmarkButton({ id, read, setRead }: Props) {
  // TODO handle error/loading states
  const { mutate } = useSetNotificationStatus(id);

  const setStatus = async (status: NotificationStatus) => {
    mutate(
      { status },
      {
        onSuccess: () => {
          setRead(status === NotificationStatus.DONE);
        },
      }
    );
  };

  return read ? (
    <Button
      onClick={() => setStatus(NotificationStatus.NOT_DONE)}
      className={classNames(styles.button, styles.read)}
      size="medium"
    >
      <CheckmarkCircleFillIcon fontSize={"2rem"} className={styles.icon} />
    </Button>
  ) : (
    <Button
      onClick={() => setStatus(NotificationStatus.DONE)}
      className={classNames(styles.button, styles.unread)}
      size="medium"
    >
      <CheckmarkCircleIcon fontSize={"2rem"} className={styles.icon} />
    </Button>
  );
}
