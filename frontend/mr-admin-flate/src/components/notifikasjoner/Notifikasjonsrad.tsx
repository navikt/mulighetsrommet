import { BodyLong, BodyShort, Heading } from "@navikt/ds-react";
import classNames from "classnames";
import { UserNotification } from "mulighetsrommet-api-client";
import { useState } from "react";
import { formaterDatoTid } from "../../utils/Utils";
import { CheckmarkButton } from "./CheckmarkButton";
import styles from "./Notifikasjoner.module.scss";

interface NotifikasjonssradProps {
  notifikasjon: UserNotification;
  lest: boolean;
}

export function Notifikasjonssrad({
  notifikasjon,
  lest,
}: NotifikasjonssradProps) {
  const { title, description, createdAt } = notifikasjon;

  const [read, setRead] = useState<boolean>(lest);

  return (
    <li
      className={classNames(
        styles.list_element,
        lest ? styles.leste : styles.uleste
      )}
    >
      <div className={styles.flex}>
        <Heading
          level="2"
          size="small"
          title={title}
          className={styles.truncate}
        >
          {title}
        </Heading>
        <BodyLong size="small">{description}</BodyLong>
        <BodyShort size="small" title={createdAt} className={styles.muted}>
          {formaterDatoTid(createdAt)}
        </BodyShort>
      </div>
      <div>
        <CheckmarkButton id={notifikasjon.id} read={read} setRead={setRead} />
      </div>
    </li>
  );
}
