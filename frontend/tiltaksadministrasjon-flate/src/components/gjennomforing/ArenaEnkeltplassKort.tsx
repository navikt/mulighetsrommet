import { useState } from "react";
import { MigrationIcon } from "@navikt/aksel-icons";
import { BodyShort, Box, Heading, HStack, Tag, Button, VStack } from "@navikt/ds-react";
import { ArenaEnkeltplassDto } from "@tiltaksadministrasjon/api-client";
import { MigrerEnkeltplassModal } from "./MigrerEnkeltplassModal";

export function ArenaEnkeltplassKort({ kandidat }: { kandidat: ArenaEnkeltplassDto }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <Box
        borderRadius="12"
        borderWidth="1"
        borderColor="neutral-subtle"
        background="raised"
        padding="space-24"
      >
        <HStack gap="space-24" align="center" justify="space-between">
          <VStack gap="space-16" align="start">
            <VStack gap="space-4">
              <Heading level="3" size="small">
                {kandidat.tiltakstype.navn} ({kandidat.tiltaksnummer})
              </Heading>
              <BodyShort textColor="subtle">
                {kandidat.arrangor.navn} ({kandidat.arrangor.organisasjonsnummer})
              </BodyShort>
              <BodyShort>
                Denne enkeltplassen er i Arena og må overføres for å kunne gjøre økonomibehandling i
                Tiltaksadministrasjon.
              </BodyShort>
            </VStack>
            <Tag
              size="small"
              variant="moderate"
              data-color="meta-purple"
              icon={<MigrationIcon aria-hidden />}
            >
              Må overføres
            </Tag>
          </VStack>
          <Button
            size="small"
            variant="primary"
            data-color="accent"
            icon={<MigrationIcon aria-hidden />}
            aria-label={`Overfør ${kandidat.tiltaksnummer} fra Arena`}
            onClick={() => setOpen(true)}
          >
            Overfør fra Arena
          </Button>
        </HStack>
      </Box>
      {open && <MigrerEnkeltplassModal kandidat={kandidat} onClose={() => setOpen(false)} />}
    </>
  );
}
