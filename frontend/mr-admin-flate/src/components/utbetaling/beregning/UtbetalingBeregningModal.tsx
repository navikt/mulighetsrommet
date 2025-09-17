import { Heading, HGrid, Modal, VStack } from "@navikt/ds-react";
import { NavEnhetDto } from "@tiltaksadministrasjon/api-client";
import { NavEnhetFilter } from "@mr/frontend-common";
import { Suspense, useState } from "react";
import { Laster } from "@/components/laster/Laster";
import UtbetalingBeregning from "./UtbetalingBeregning";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useUtbetalingBeregning } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";

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
  const { data: enheter } = useNavEnheter();
  const [navEnheter, setNavEnheter] = useState<NavEnhetDto[]>([]);
  const { data: beregning } = useUtbetalingBeregning(
    { navEnheter: navEnheter.map((e) => e.enhetsnummer) },
    utbetalingId,
  );

  return (
    <VStack>
      <HGrid columns="20% 1fr" gap="2" align="start">
        <VStack>
          <NavEnhetFilter
            value={navEnheter}
            onChange={(navEnheter) => {
              return setNavEnheter(
                enheter.filter((enhet) => navEnheter.includes(enhet.enhetsnummer)),
              );
            }}
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
