import { Heading, HStack, Modal, VStack } from "@navikt/ds-react";
import { Suspense, useState } from "react";
import { Laster } from "@/components/laster/Laster";
import UtbetalingBeregning from "./UtbetalingBeregning";
import { useUtbetalingBeregning } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { NavEnhetFilter } from "@/components/filter/NavEnhetFilter";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  utbetalingId: string;
}

export function UtbetalingBeregningModal({ utbetalingId, modalOpen, onClose }: Props) {
  const [navEnheter, setNavEnheter] = useState<string[]>([]);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter }, utbetalingId);
  return (
    <Modal open={modalOpen} onClose={onClose} aria-label="modal" width="80rem" className="h-240">
      <Modal.Header closeButton>
        <Heading size="medium" level="2">
          Beregning
        </Heading>
      </Modal.Header>
      <Modal.Body>
        <Suspense fallback={<Laster tekst="Laster..." />}>
          <HStack gap="space-16" justify="space-between">
            <VStack>
              <Heading size="xsmall" level="3" spacing>
                Oppfølgingsenhet
              </Heading>
              <NavEnhetFilter
                value={navEnheter}
                onChange={setNavEnheter}
                regioner={beregning.deltakerRegioner}
              />
            </VStack>
            <UtbetalingBeregning beregning={beregning} />
          </HStack>
        </Suspense>
      </Modal.Body>
    </Modal>
  );
}
