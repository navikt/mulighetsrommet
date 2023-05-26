import {
  CheckmarkCircleFillIcon,
  CheckmarkCircleIcon,
} from "@navikt/aksel-icons";
import { mulighetsrommetClient } from "../../api/clients";
import classNames from "classnames";
import styles from "./CheckmarkButton.module.scss";
import { useNotificationSummary } from "../../api/notifikasjoner/useNotificationSummary";

interface Props {
  id: string;
  read: boolean;
  setRead: React.Dispatch<React.SetStateAction<boolean>>;
}

export function CheckmarkButton({ id, read, setRead }: Props) {
  const { refetch } = useNotificationSummary();
  const markUnread = async () => {
    try {
      await mulighetsrommetClient.notifications.markAsUnread({ id });
      await refetch();
    } catch (e) {
      return;
    }
    setRead(false);
  };

  const markRead = async () => {
    try {
      await mulighetsrommetClient.notifications.markAsRead({ id });
      await refetch();
    } catch (e) {
      return;
    }
    setRead(true);
  };

  return read ? (
    <CheckmarkCircleFillIcon
      fontSize={"1.5rem"}
      onClick={markUnread}
      className={classNames(styles.button, styles.greenFill)}
    />
  ) : (
    <CheckmarkCircleIcon
      fontSize={"1.5rem"}
      onClick={markRead}
      className={styles.button}
    />
  );
}
