import { Heading, HStack, Modal, VStack } from "@navikt/ds-react";
import { Suspense, useEffect, useState } from "react";
import { Laster } from "@/components/laster/Laster";
import UtbetalingBeregning from "./UtbetalingBeregning";
import { useUtbetalingBeregning } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { NavEnhetFilter } from "@/components/filter/NavEnhetFilter";
import { Kontorstruktur } from "@tiltaksadministrasjon/api-client";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  utbetalingId: string;
}

export function UtbetalingBeregningModal({ utbetalingId, modalOpen, onClose }: Props) {
  const [navEnheter, setNavEnheter] = useState<string[]>([]);
  const [regioner, setRegioner] = useState<Kontorstruktur[]>([]);
  return (
    <Modal open={modalOpen} onClose={onClose} aria-label="modal" width="80rem" className="h-240">
      <Modal.Header closeButton>
        <Heading size="medium" level="2">
          Beregning
        </Heading>
      </Modal.Header>
      <Modal.Body>
        <HStack gap="space-16" justify="space-between">
          <VStack>
            <Heading size="xsmall" level="3" spacing>
              Oppfølgingsenhet
            </Heading>
            <NavEnhetFilter value={navEnheter} onChange={setNavEnheter} regioner={regioner} />
          </VStack>
          <Suspense fallback={<Laster tekst="Laster..." />}>
            <UtbetalingBeregningInner
              utbetalingId={utbetalingId}
              navEnheter={navEnheter}
              setRegioner={setRegioner}
            />
          </Suspense>
        </HStack>
      </Modal.Body>
    </Modal>
  );
}
interface UtbetalingBeregningInnerProps {
  utbetalingId: string;
  navEnheter: string[];
  setRegioner: (data: Kontorstruktur[]) => void;
}

function UtbetalingBeregningInner({
  utbetalingId,
  navEnheter,
  setRegioner,
}: UtbetalingBeregningInnerProps) {
  const { data: beregning } = useUtbetalingBeregning({ navEnheter }, utbetalingId);
  useEffect(() => {
    setRegioner(beregning.deltakerRegioner);
  }, [beregning.deltakerRegioner]);
  return <UtbetalingBeregning beregning={beregning} />;
}
