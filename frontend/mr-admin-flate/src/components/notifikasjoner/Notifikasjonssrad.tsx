/* eslint-disable camelcase */
import classNames from "classnames";
import { UserNotification } from "mulighetsrommet-api-client";
import styles from "./Notifikasjonsrad.module.scss";
import { BodyShort } from "@navikt/ds-react";

export function formaterDatoTid(dato: string | Date, fallback = ""): string {
  const result = new Date(dato).toLocaleTimeString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  if (result === "Invalid Date") {
    return fallback;
  }

  return result.replace(",", " -");
}

interface Props {
  notifikasjon: UserNotification;
  index: number;
}

export function Notifikasjonssrad({ notifikasjon, index }: Props) {
  const { id, type, title, description, user, createdAt, readAt } =
    notifikasjon;

  return (
    <li className={styles.list_element} id={`list_element_${index}`}>
      <div className={styles.gjennomforing_container}>
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
      </div>
    </li>
  );
}
