import {
  CheckmarkCircleFillIcon,
  CheckmarkCircleIcon,
} from "@navikt/aksel-icons";
import { mulighetsrommetClient } from "../../api/clients";
import classNames from "classnames";
import styles from "./CheckmarkButton.module.scss";

interface Props {
  id: string;
  read: boolean;
  setRead: React.Dispatch<React.SetStateAction<boolean>>;
}

export function CheckmarkButton({ id, read, setRead }: Props) {
  const markUnread = () => {
    try {
      mulighetsrommetClient.notifications.markAsUnread({ id });
    } catch (e) {
      return;
    }
    setRead(false);
  };

  const markRead = () => {
    try {
      mulighetsrommetClient.notifications.markAsRead({ id });
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
