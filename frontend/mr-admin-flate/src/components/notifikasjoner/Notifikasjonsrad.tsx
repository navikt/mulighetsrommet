import { Alert, BodyLong, BodyShort, Heading, Link, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { NotificationType, UserNotification } from "@mr/api-client-v2";
import { ReactNode, useState } from "react";
import { formaterDatoTid } from "../../utils/Utils";
import { CheckmarkButton } from "./CheckmarkButton";
import { useRevalidator } from "react-router";

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
    <li
      className={classNames(
        "flex flex-row justify-between m-2 p-4",
        lest
          ? "border-b border-divider bg-gray-200"
          : "bg-white rounded border-b-[0.5px] border-transparent",
        "md:w-auto w-[95%]",
      )}
    >
      <div className="flex flex-col gap-2 max-w-[75ch]">
        <BodyShort>{tag(type, lest)}</BodyShort>
        <Heading
          level="2"
          size="small"
          title={title}
          className={classNames("overflow-hidden overflow-wrap-normal", { "line-through": read })}
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
      <div className="flex items-start gap-1">
        <BodyShort size="small" title={createdAt} className="text-subtle text-sm">
          {formaterDatoTid(createdAt)}
        </BodyShort>
        <VStack className="flex flex-row">
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
