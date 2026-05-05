import { useOppskrifter } from "@/api/queries/useOppskrifter";
import { BodyLong, Box, Button, Heading, HGrid, Link, List, VStack } from "@navikt/ds-react";
import { useEffect, useRef } from "react";
import { APPLICATION_WEB_COMPONENT_NAME } from "@/constants";
import { Melding } from "../melding/Melding";
import { PortableText } from "@mr/frontend-common";
import { Tiltakskode } from "@api-client";
import { XMarkIcon } from "@navikt/aksel-icons";

interface Props {
  oppskriftId: string;
  tiltakskode: Tiltakskode;
  setOppskriftId: (id: string | undefined) => void;
}

export function Oppskrift({ oppskriftId, tiltakskode, setOppskriftId }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const { data: oppskrifter } = useOppskrifter(tiltakskode);

  useEffect(() => {
    if (ref.current) {
      ref.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [oppskriftId]);

  if (!oppskrifter) return null;

  const oppskrift = oppskrifter.data.find((oppskrift) => oppskrift._id === oppskriftId);

  if (!oppskrift) {
    return (
      <Melding header="Varsel" variant="warning">
        Vi kunne dessverre ikke finne oppskriften
      </Melding>
    );
  }

  function navigateViaShadowDomToElement(elementId: string) {
    // Siden vi bruker en shadow-dom når vi bygger appen som en web-component så fungerer ikke
    // vanlig navigering med anchor tags og id'er så vi må bruke querySelector for å
    // hente ut elementet enten via shadow-dom (via ?.shadowRoot) eller direkte for så å scrolle til elementet
    const shadowDom = document.querySelector(APPLICATION_WEB_COMPONENT_NAME)?.shadowRoot;
    if (shadowDom) {
      // Dette skjer når vi bygger appen som en web-component
      shadowDom.querySelector(elementId)?.scrollIntoView({ behavior: "smooth" });
    } else {
      // Dette skjer ved lokal kjøring av appen
      document.querySelector(elementId)?.scrollIntoView({ behavior: "smooth" });
    }
  }

  return (
    <Box
      padding="space-16"
      background="info-soft"
      borderWidth="1"
      borderColor="info"
      marginBlock="space-16"
    >
      <VStack gap="space-16" align="start">
        <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => setOppskriftId(undefined)}>
          Lukk oppskriften
        </Button>
        <Heading level="3" size="medium" spacing>
          {oppskrift.navn}
        </Heading>
        <BodyLong spacing>{oppskrift.beskrivelse}</BodyLong>
      </VStack>
      <HGrid gap="space-16" columns="1fr 0.25fr">
        <section ref={ref} className="overflow-y-scroll max-h-200">
          {oppskrift.steg.map((st, index) => {
            return (
              <Box key={st.navn} background="default" padding="space-16" margin="space-16">
                <Heading
                  level="4"
                  size="small"
                  id={`steg-${index + 1}`}
                >{`${index + 1}. ${st.navn}`}</Heading>
                <PortableText value={st.innhold} />
              </Box>
            );
          })}
        </section>
        <aside className="mx-4">
          <nav>
            <List>
              {oppskrift.steg.map((s, index) => {
                return (
                  <List.Item key={s.navn}>
                    <Link
                      aria-label={`Naviger til steg: ${index + 1}`}
                      href={`#steg-${index + 1}`}
                      onClick={() => navigateViaShadowDomToElement(`#steg-${index + 1}`)}
                    >
                      {s.navn}
                    </Link>
                  </List.Item>
                );
              })}
            </List>
          </nav>
        </aside>
      </HGrid>
    </Box>
  );
}
