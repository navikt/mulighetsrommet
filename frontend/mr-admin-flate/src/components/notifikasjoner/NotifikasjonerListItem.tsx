import { UserNotification } from "@tiltaksadministrasjon/api-client";
import {
  Alert,
  BodyLong,
  BodyShort,
  Box,
  Heading,
  HStack,
  Link,
  Spacer,
  VStack,
} from "@navikt/ds-react";
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
    <li>
      <Box
        background={lest ? "sunken" : "soft"}
        borderColor="neutral-subtle"
        borderRadius="8"
        borderWidth="1"
        padding="space-16"
      >
        <VStack gap="space-16">
          <HStack gap="space-16" align="end">
            <Box
              padding="space-2"
              background="default"
              borderColor="neutral"
              borderRadius="8"
              borderWidth="1"
            >
              <PaperplaneIcon fontSize="2rem" />
            </Box>
            <Heading
              level="3"
              size="small"
              title={title}
              className={`${lest ? "line-through" : ""}`}
            >
              {title}
            </Heading>
            <Spacer />
            <ReadNotificationButton id={notifikasjon.id} read={lest} setError={setError} />
          </HStack>
          {description && (
            <BodyLong size="small" spacing>
              {description}
            </BodyLong>
          )}
          <HStack gap="space-8" align="start">
            {metadata?.link && metadata.linkText ? (
              <BodyShort size="small">
                <Link href={metadata.link}>{metadata.linkText}</Link>
              </BodyShort>
            ) : null}
            <Spacer />
            <BodyShort size="small" title={createdAt}>
              {formaterDatoTid(createdAt)}
            </BodyShort>
            {error && (
              <Alert inline variant="error" size="small">
                {error}
              </Alert>
            )}
          </HStack>
        </VStack>
      </Box>
    </li>
  );
}
