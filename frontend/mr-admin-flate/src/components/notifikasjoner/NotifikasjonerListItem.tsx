import { UserNotification } from "@mr/api-client-v2";
import {
  Alert,
  BodyLong,
  BodyShort,
  Box,
  Heading,
  HStack,
  Link,
  Tag,
  VStack,
} from "@navikt/ds-react";
import classNames from "classnames";
import { useState } from "react";
import { ReadNotificationButton } from "./ReadNotificationButton";
import { PaperplaneIcon } from "@navikt/aksel-icons";
import { formaterDatoTid } from "@mr/frontend-common/utils/date";

interface NotifikasjonerListItemProps {
  notifikasjon: UserNotification;
  lest: boolean;
}

export function NotifikasjonerListItem({ notifikasjon, lest }: NotifikasjonerListItemProps) {
  const { title, description, createdAt, metadata } = notifikasjon;

  const [error, setError] = useState("");

  return (
    <li className="m-2 md:w-auto w-[95%]">
      <Box
        background={lest ? "surface-subtle" : "bg-default"}
        borderColor="border-subtle"
        borderRadius="large"
        borderWidth="1"
        padding="4"
      >
        <HStack justify="space-between">
          <HStack gap="2">
            <div className="inline-flex items-center self-start justify-center p-2 bg-gray-200 rounded-xl">
              <PaperplaneIcon fontSize="2rem" />
            </div>
            <VStack gap="4" className="max-w-[75ch]">
              <Heading
                level="2"
                size="small"
                title={title}
                className={classNames("overflow-hidden overflow-wrap-normal", {
                  "line-through": lest,
                })}
              >
                {title}
              </Heading>
              <BodyLong size="small">{description}</BodyLong>

              <HStack justify="start">
                <Tag size="xsmall" variant="warning">
                  Notifikasjon
                </Tag>
              </HStack>
              {metadata?.link && metadata.linkText ? (
                <BodyShort size="small">
                  <Link href={metadata.link}>{metadata.linkText}</Link>
                </BodyShort>
              ) : null}
            </VStack>
          </HStack>
          <VStack className="flex items-end justify-between gap-4">
            <ReadNotificationButton id={notifikasjon.id} read={lest} setError={setError} />
            <BodyShort size="small" title={createdAt} className="text-subtle text-sm">
              {formaterDatoTid(createdAt)}
            </BodyShort>
            {error && (
              <Alert inline variant="error" size="small">
                {error}
              </Alert>
            )}
          </VStack>
        </HStack>
      </Box>
    </li>
  );
}
