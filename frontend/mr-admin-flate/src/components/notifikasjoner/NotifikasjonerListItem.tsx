import { UserNotification } from "@mr/api-client-v2";
import { Alert, BodyLong, BodyShort, Heading, Link, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { useState } from "react";
import { formaterDatoTid } from "@/utils/Utils";
import { ReadNotificationButton } from "./ReadNotificationButton";

interface NotifikasjonerListItemProps {
  notifikasjon: UserNotification;
  lest: boolean;
}

export function NotifikasjonerListItem({ notifikasjon, lest }: NotifikasjonerListItemProps) {
  const { title, description, createdAt, metadata } = notifikasjon;

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
        <BodyShort>
          <Tag size="xsmall" variant="warning">
            Notifikasjon
          </Tag>
        </BodyShort>
        <Heading
          level="2"
          size="small"
          title={title}
          className={classNames("overflow-hidden overflow-wrap-normal", { "line-through": lest })}
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
      <VStack className="flex items-end justify-between gap-4">
        <BodyShort size="small" title={createdAt} className="text-subtle text-sm">
          {formaterDatoTid(createdAt)}
        </BodyShort>
        <ReadNotificationButton id={notifikasjon.id} read={lest} setError={setError} />
        {error && (
          <Alert inline variant="error" size="small">
            {error}
          </Alert>
        )}
      </VStack>
    </li>
  );
}
