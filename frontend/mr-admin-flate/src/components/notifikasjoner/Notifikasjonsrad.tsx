import { BodyLong, BodyShort, Heading, Link, Tag } from "@navikt/ds-react";
import classNames from "classnames";
import { NotificationType, UserNotification } from "mulighetsrommet-api-client";
import { ReactNode, useState } from "react";
import { Slide, ToastContainer } from "react-toastify";
import { formaterDatoTid } from "../../utils/Utils";
import { CheckmarkButton } from "./CheckmarkButton";
import styles from "./Notifikasjoner.module.scss";

interface NotifikasjonssradProps {
  notifikasjon: UserNotification;
  lest: boolean;
}

function tag(type: NotificationType, lest: boolean): ReactNode {
  switch (type) {
    case NotificationType.NOTIFICATION:
      return (
        <Tag size="xsmall" variant={lest ? "warning-moderate" : "warning-filled"}>
          Notifikasjon
        </Tag>
      );
    case NotificationType.TASK:
      return (
        <Tag size="xsmall" variant={lest ? "info-moderate" : "info-filled"}>
          Oppgave
        </Tag>
      );
  }
}

export function Notifikasjonssrad({ notifikasjon, lest }: NotifikasjonssradProps) {
  const { title, description, createdAt, type, metadata } = notifikasjon;

  const [read, setRead] = useState<boolean>(lest);

  return (
    <li className={classNames(styles.list_element, lest ? styles.leste : styles.uleste)}>
      <div className={styles.flex}>
        <BodyShort>{tag(type, lest)}</BodyShort>
        <Heading level="2" size="small" title={title} className={styles.truncate}>
          {title}
        </Heading>
        <BodyLong size="small">{description}</BodyLong>
        {metadata?.link && metadata?.linkText ? (
          <BodyShort size="small">
            <Link href={metadata.link}>{metadata.linkText}</Link>
          </BodyShort>
        ) : null}
      </div>
      <div className={styles.right}>
        <BodyShort size="small" title={createdAt} className={styles.muted}>
          {formaterDatoTid(createdAt)}
        </BodyShort>
        <CheckmarkButton id={notifikasjon.id} read={read} setRead={setRead} />
        <ToastContainer
          position="bottom-left"
          newestOnTop={true}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
          transition={Slide}
        />
      </div>
    </li>
  );
}
