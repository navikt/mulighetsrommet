import { BodyLong, BodyShort, Heading, Tag } from "@navikt/ds-react";
import classNames from "classnames";
import { NotificationType, UserNotification } from "mulighetsrommet-api-client";
import { ReactNode, useState } from "react";
import { Slide, ToastContainer } from "react-toastify";
import { formaterDatoTid } from "../../utils/Utils";
import { CheckmarkButton } from "./CheckmarkButton";
import styles from "./Notifikasjoner.module.scss";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface NotifikasjonssradProps {
  notifikasjon: UserNotification;
  lest: boolean;
}

function tag(type: NotificationType, lest: boolean): ReactNode {
  switch (type) {
    case NotificationType.NOTIFICATION:
      return (
        <Tag
          data-testid="notifikasjon-tag"
          variant={lest ? "warning-moderate" : "warning-filled"}
        >
          Notifikasjon
        </Tag>
      );
    case NotificationType.TASK:
      return (
        <Tag
          data-testid="oppgave-tag"
          variant={lest ? "info-moderate" : "info-filled"}
        >
          Oppgave
        </Tag>
      );
  }
}

export function Notifikasjonssrad({
  notifikasjon,
  lest,
}: NotifikasjonssradProps) {
  const { title, description, createdAt, type } = notifikasjon;

  const [read, setRead] = useState<boolean>(lest);

  return (
    <li
      className={classNames(
        styles.list_element,
        lest ? styles.leste : styles.uleste,
      )}
      data-testid="notifikasjon"
    >
      <div className={styles.flex}>
        <Heading
          level="2"
          size="small"
          title={title}
          className={classNames(styles.truncate, styles.header)}
        >
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {title || ""}
          </ReactMarkdown>
        </Heading>
        <BodyLong size="small">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {description || ""}
          </ReactMarkdown>
        </BodyLong>
        <BodyShort size="small" title={createdAt} className={styles.muted}>
          {formaterDatoTid(createdAt)}
        </BodyShort>
        <BodyShort>{tag(type, lest)}</BodyShort>
      </div>
      <div>
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
