import { Alert, BodyLong, BodyShort, Heading, Link, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { NotificationType, UserNotification } from "@mr/api-client";
import { ReactNode, useState } from "react";
import { formaterDatoTid } from "../../utils/Utils";
import { CheckmarkButton } from "./CheckmarkButton";
import styles from "./Notifikasjoner.module.scss";
import { useRevalidator } from "react-router-dom";

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
  const revalidator = useRevalidator();

  const [read, setRead] = useState<boolean>(lest);
  const [error, setError] = useState("");

  return (
    <li className={classNames(styles.list_element, lest ? styles.leste : styles.uleste)}>
      <div className={styles.flex}>
        <BodyShort>{tag(type, lest)}</BodyShort>
        <Heading
          level="2"
          size="small"
          title={title}
          className={classNames(styles.truncate, {
            [styles.read]: read,
          })}
        >
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
        <VStack>
          <CheckmarkButton
            id={notifikasjon.id}
            read={read}
            setRead={(value) => {
              setRead(value);
              revalidator.revalidate();
            }}
            setError={setError}
          />
          {error && (
            <Alert inline variant="error" size="small">
              {error}
            </Alert>
          )}
        </VStack>
      </div>
    </li>
  );
}
