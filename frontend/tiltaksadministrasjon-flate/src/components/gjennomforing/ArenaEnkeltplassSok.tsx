import { BodyShort, Button, Heading, HStack, InfoCard, VStack } from "@navikt/ds-react";
import { ExclamationmarkTriangleIcon, XMarkOctagonIcon } from "@navikt/aksel-icons";
import { GjennomforingHandling } from "@tiltaksadministrasjon/api-client";
import {
  useEnkeltplassHandlinger,
  useFinnArenaEnkeltplass,
} from "@/api/gjennomforing/useMigrerEnkeltplass";
import { ArenaEnkeltplassKort } from "./ArenaEnkeltplassKort";

export function ArenaEnkeltplassSok({ search }: { search: string }) {
  const { data: handlinger } = useEnkeltplassHandlinger();
  const kanMigrere = handlinger.includes(GjennomforingHandling.MIGRER_ENKELTPLASS);
  const { data, error, refetch } = useFinnArenaEnkeltplass(search, kanMigrere);

  if (!kanMigrere) {
    return null;
  }

  if (error) {
    return (
      <InfoCard
        data-color={error.status === 403 ? "warning" : "danger"}
        role="alert"
        className="mb-4"
      >
        <InfoCard.Message
          icon={
            error.status === 403 ? (
              <ExclamationmarkTriangleIcon aria-hidden />
            ) : (
              <XMarkOctagonIcon aria-hidden />
            )
          }
        >
          <VStack gap="space-12">
            <BodyShort>
              {error.status === 403
                ? error.detail
                : "Kunne ikke søke etter enkeltplasser i Arena. Prøv igjen."}
            </BodyShort>
            {error.status !== 403 && (
              <HStack>
                <Button size="small" variant="secondary" onClick={() => refetch()}>
                  Prøv igjen
                </Button>
              </HStack>
            )}
          </VStack>
        </InfoCard.Message>
      </InfoCard>
    );
  }

  if (!data?.length) {
    return null;
  }

  return (
    <VStack gap="space-24" className="my-6">
      <Heading level="2" size="small">
        Fant {data.length} {data.length === 1 ? "gjennomføring" : "gjennomføringer"} i Arena
      </Heading>
      <VStack gap="space-16">
        {data.map((kandidat) => (
          <ArenaEnkeltplassKort key={kandidat.id} kandidat={kandidat} />
        ))}
      </VStack>
    </VStack>
  );
}
