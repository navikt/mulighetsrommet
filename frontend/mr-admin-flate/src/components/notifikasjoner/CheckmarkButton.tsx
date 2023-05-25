import { Dispatch, SetStateAction } from "react";
import {
  CheckmarkCircleFillIcon,
  CheckmarkCircleIcon,
} from "@navikt/aksel-icons";
import { mulighetsrommetClient } from "../../api/clients";
import { NotificationStatus } from "mulighetsrommet-api-client";
import classNames from "classnames";
import styles from "./CheckmarkButton.module.scss";
import { useNotificationSummary } from "../../api/notifikasjoner/useNotificationSummary";

interface Props {
  id: string;
  read: boolean;
  setRead: Dispatch<SetStateAction<boolean>>;
}

export function CheckmarkButton({ id, read, setRead }: Props) {
  const { refetch } = useNotificationSummary();

  const setStatus = async (status: NotificationStatus) => {
    try {
      await mulighetsrommetClient.notifications.setNotificationStatus({
        id,
        requestBody: { status },
      });
      await refetch();
    } catch (e) {
      return;
    }

    setRead(status === NotificationStatus.DONE);
  };


  return read ? (
    <CheckmarkCircleFillIcon
      fontSize={"1.5rem"}
      onClick={() => setStatus(NotificationStatus.NOT_DONE)}
      className={classNames(styles.button, styles.greenFill)}
    />
  ) : (
    <CheckmarkCircleIcon
      fontSize={"1.5rem"}
      onClick={() => setStatus(NotificationStatus.DONE)}
      className={styles.button}
    />
  );
}
