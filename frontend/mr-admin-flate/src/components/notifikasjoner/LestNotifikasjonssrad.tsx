import { BodyLong, BodyShort, Heading } from "@navikt/ds-react";
import { UserNotification } from "mulighetsrommet-api-client";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./LestNotifikasjonsrad.module.scss";
import { useState } from "react";
import { CheckmarkButton } from "./CheckmarkButton";

interface Props {
  notifikasjon: UserNotification;
}

export function LestNotifikasjonssrad({ notifikasjon }: Props) {
  const { title, description, createdAt, readAt } = notifikasjon;

  const [read, setRead] = useState<boolean>(true);

  return (
    <li className={styles.list_element}>
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
