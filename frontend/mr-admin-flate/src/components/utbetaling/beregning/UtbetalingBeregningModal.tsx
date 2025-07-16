import { Heading, HGrid, Modal, VStack } from "@navikt/ds-react";
import { NavEnhet } from "@mr/api-client-v2";
import { NavEnhetFilter, useApiSuspenseQuery } from "@mr/frontend-common";
import { Suspense, useState } from "react";
import { Laster } from "@/components/laster/Laster";
import { beregningQuery } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import UtbetalingBeregning from "./UtbetalingBeregning";

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
        <Heading size="medium">Beregning</Heading>
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
  const [navEnheter, setNavEnheter] = useState<NavEnhet[]>([]);
  const { data: beregning } = useApiSuspenseQuery(
    beregningQuery({ navEnheter: navEnheter.map((e) => e.enhetsnummer) }, utbetalingId),
  );

  return (
    <VStack>
      <HGrid columns="20% 1fr" gap="2" align="start">
        <VStack>
          <NavEnhetFilter
            value={navEnheter}
            onChange={(enheter) =>
              setNavEnheter(
                beregning.deltakerRegioner
                  .flatMap((region) => region.enheter)
                  .filter((enhet) => enheter.includes(enhet.enhetsnummer)),
              )
            }
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
