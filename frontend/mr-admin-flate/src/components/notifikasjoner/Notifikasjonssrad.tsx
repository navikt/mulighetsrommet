import classNames from "classnames";
import { UserNotification } from "mulighetsrommet-api-client";
import styles from "./Notifikasjonsrad.module.scss";
import { BodyShort } from "@navikt/ds-react";
import { CheckmarkCircleIcon } from "@navikt/aksel-icons";
import {formaterDatoTid} from "../../utils/Utils";

interface Props {
  notifikasjon: UserNotification;
}

export function Notifikasjonssrad({ notifikasjon }: Props) {
  const { id, type, title, description, user, createdAt, readAt } =
    notifikasjon;

  return (
    <li className={styles.list_element} id={id}>
      <div className={styles.notifikasjon_container}>
        <div className={classNames(styles.flex, styles.navn)}>
          <BodyShort
            size="small"
            title={title}
            className={classNames(styles.truncate, styles.bold)}
          >
            {`${title} - ${description}`}
          </BodyShort>
          <BodyShort size="small" title={createdAt} className={styles.muted}>
            {formaterDatoTid(createdAt)}
          </BodyShort>
        </div>
        <CheckmarkCircleIcon
          fillOpacity={readAt == null ? 0.4 : 1}
          fontSize={"1.5rem"}
        />
      </div>
    </li>
  );
}
