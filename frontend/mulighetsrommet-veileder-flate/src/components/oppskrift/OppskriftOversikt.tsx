import {
  BodyLong,
  BodyShort,
  Box,
  Heading,
  HGrid,
  Skeleton,
  Spacer,
  VStack,
} from "@navikt/ds-react";
import { Tiltakskode } from "@api-client";
import { useOppskrifter } from "@/api/queries/useOppskrifter";
import { formaterDato } from "@/utils/Utils";
import { Suspense } from "react";
import { Melding } from "../melding/Melding";

interface Props {
  tiltakskode: Tiltakskode;
  setOppskriftId: (id: string) => void;
}

export function OppskriftOversikt({ tiltakskode, setOppskriftId }: Props) {
  const { data: oppskrifter } = useOppskrifter(tiltakskode);

  if (!oppskrifter) return null;

  if (oppskrifter.data.length === 0) {
    return (
      <Melding header="Ingen oppskrifter" variant="info">
        Det er ikke lagt inn oppskrifter for denne tiltakstypen
      </Melding>
    );
  }

  return (
    <Suspense fallback={<Skeleton variant="rectangle" width="15rem" height={200} />}>
      <HGrid columns={{ xs: "repeat(auto-fit, minmax(1rem, 1fr))" }} gap="space-16">
        {oppskrifter.data.map((o) => {
          return (
            <Box
              as="button"
              type="button"
              key={o._id}
              background="info-moderate"
              padding="space-16"
              borderColor="info"
              borderWidth="1"
              borderRadius="2"
              onClick={() => setOppskriftId(o._id)}
              className="hover:bg-ax-info-100 cursor-pointer text-left"
            >
              <VStack height="100%" align="start">
                <Heading level="4" size="xsmall" spacing>
                  {o.navn}
                </Heading>
                <BodyLong spacing>{o.beskrivelse}</BodyLong>
                <Spacer />
                <BodyShort size="small">
                  Oppdatert: {formaterDato(new Date(o._updatedAt))}
                </BodyShort>
              </VStack>
            </Box>
          );
        })}
      </HGrid>
    </Suspense>
  );
}
