import { Heading, HGrid, Modal, VStack } from "@navikt/ds-react";
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
  return (
    <Modal
      open={modalOpen}
      onClose={onClose}
      aria-label="modal"
      width="80rem"
      className="h-[60rem]"
    >
      <Modal.Header closeButton>
        <Heading size="medium" level="2">
          Beregning
        </Heading>
      </Modal.Header>
      <Modal.Body>
        <Suspense fallback={<Laster tekst="Laster..." />}>
          <ModalBody utbetalingId={utbetalingId} />
        </Suspense>
      </Modal.Body>
    </Modal>
  );
}

interface BodyProps {
  utbetalingId: string;
}

function ModalBody({ utbetalingId }: BodyProps) {
  const [navEnheter, setNavEnheter] = useState<string[]>([]);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter }, utbetalingId);

  return (
    <VStack>
      <HGrid columns="20% 1fr" gap="2" align="start">
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
        <VStack>
          <div className={`max-h-[50rem] overflow-y-scroll`}>
            <UtbetalingBeregning beregning={beregning} />
          </div>
        </VStack>
      </HGrid>
    </VStack>
  );
}
