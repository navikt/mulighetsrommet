import {
  BodyLong,
  BodyShort,
  ExpansionCard,
  Heading,
  Skeleton,
  Spacer,
  VStack,
} from "@navikt/ds-react";
import { Tiltakskode } from "@arbeidsmarkedstiltak/api-client";
import { useOppskrifter } from "@/api/queries/useOppskrifter";
import { formaterDato } from "@/utils/Utils";
import { Suspense } from "react";
import { Melding } from "../melding/Melding";
import { Oppskrift } from "./Oppskrift";

interface Props {
  tiltakskode: Tiltakskode;
}

export function OppskriftOversikt({ tiltakskode }: Props) {
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
      <VStack gap="space-16">
        {oppskrifter.data.map((o) => {
          return (
            <ExpansionCard
              key={o._id}
              data-color="info"
              className="hover:bg-ax-info-100 cursor-pointer text-left"
              aria-label={o.navn}
            >
              <ExpansionCard.Header>
                <ExpansionCard.Title>
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
                </ExpansionCard.Title>
              </ExpansionCard.Header>
              <ExpansionCard.Content>
                <Oppskrift oppskriftId={o._id} tiltakskode={tiltakskode} />
              </ExpansionCard.Content>
            </ExpansionCard>
          );
        })}
      </VStack>
    </Suspense>
  );
}
