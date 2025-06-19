import { Heading, HGrid, Modal, VStack } from "@navikt/ds-react";
import { DeltakerForKostnadsfordeling, NavEnhet, NavRegion } from "@mr/api-client-v2";
import { NavEnhetFilter } from "@mr/frontend-common";
import { ForhandsgodkjentDeltakerTable } from "@/components/utbetaling/ForhandsgodkjentDeltakerTable";
import { useMemo, useState } from "react";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  deltakere: DeltakerForKostnadsfordeling[];
  sats: number;
  heading: string;
}

export function ForhandsgodkjentDeltakerTableModal({
  heading,
  sats,
  deltakere,
  modalOpen,
  onClose,
}: Props) {
  const [navEnheter, setNavEnheter] = useState<NavEnhet[]>([]);

  const unikeEnheter = Array.from(
    new Map(
      deltakere
        .map((d) => d.geografiskEnhet)
        .filter((x): x is NonNullable<typeof x> => x != null)
        .map((enhet) => [enhet.enhetsnummer, enhet]),
    ).values(),
  );

  const filteredDeltakere = useMemo(() => {
    if (navEnheter.length === 0) {
      return deltakere;
    } else {
      return deltakere
        .filter((x): x is NonNullable<typeof x> => x != null)
        .filter(
          (d) =>
            d.geografiskEnhet &&
            navEnheter.map((e) => e.enhetsnummer).includes(d.geografiskEnhet.enhetsnummer),
        );
    }
  }, [deltakere, navEnheter]);

  function regioner(): NavRegion[] {
    const map: { [enhetsnummer: string]: NavRegion } = {};
    deltakere
      .map((d) => d.region)
      .filter((x): x is NonNullable<typeof x> => x != null)
      .forEach((region: NavEnhet) => {
        map[region.enhetsnummer] = {
          ...region,
          enheter: [],
        };
      });

    unikeEnheter.forEach((enhet: NavEnhet) => {
      if (!enhet.overordnetEnhet) return;

      map[enhet.overordnetEnhet].enheter.push(enhet);
    });

    return Object.values(map);
  }

  return (
    <Modal
      open={modalOpen}
      onClose={onClose}
      aria-label="modal"
      width="80rem"
      className="h-[60rem]"
    >
      <Modal.Header closeButton>
        <Heading size="medium">Deltakere i utbetalingsperiode</Heading>
      </Modal.Header>
      <Modal.Body>
        <VStack>
          <HGrid columns="20% 1fr" gap="2" align="start">
            <VStack>
              <NavEnhetFilter
                navEnheter={navEnheter}
                setNavEnheter={(enheter: string[]) => {
                  setNavEnheter(
                    unikeEnheter.filter((enhet) => enheter.includes(enhet.enhetsnummer)),
                  );
                }}
                regioner={regioner()}
              />
            </VStack>
            <VStack>
              <Heading size="small">{heading}</Heading>
              <ForhandsgodkjentDeltakerTable
                maxHeight="50rem"
                sats={sats}
                deltakere={filteredDeltakere}
              />
            </VStack>
          </HGrid>
        </VStack>
      </Modal.Body>
    </Modal>
  );
}
