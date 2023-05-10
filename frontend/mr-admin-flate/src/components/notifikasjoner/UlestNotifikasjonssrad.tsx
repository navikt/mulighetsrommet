import { CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { BodyLong, BodyShort, Heading } from "@navikt/ds-react";
import { UserNotification } from "mulighetsrommet-api-client";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./UlestNotifikasjonsrad.module.scss";

interface Props {
  notifikasjon: UserNotification;
}

export function UlestNotifikasjonssrad({ notifikasjon }: Props) {
  const { title, description, createdAt, readAt } = notifikasjon;

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
        <CheckmarkCircleIcon
          fillOpacity={readAt == null ? 0.4 : 1}
          fontSize={"1.5rem"}
        />
      </div>
    </li>
  );
}
