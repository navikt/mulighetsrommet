import { Button, HStack, VStack } from "@navikt/ds-react";
import { UtbetalingBeregningDto } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";
import UtbetalingBeregning from "./UtbetalingBeregning";
import { UtbetalingBeregningModal } from "./UtbetalingBeregningModal";

interface Props {
  beregning: UtbetalingBeregningDto;
  utbetalingId: string;
}

export default function UtbetalingBeregningView({ beregning, utbetalingId }: Props) {
  const [beregningModalOpen, setBeregningModalOpen] = useState<boolean>(false);

  return (
    <VStack gap="2">
      <div className="max-h-[30rem] overflow-y-scroll">
        <UtbetalingBeregning beregning={beregning} />
      </div>
      <HStack justify="start" align="start">
        {beregning.deltakerTableData.rows.length > 0 && (
          <Button variant="secondary" size="small" onClick={() => setBeregningModalOpen(true)}>
            Filtreringshjelp
          </Button>
        )}
      </HStack>
      <UtbetalingBeregningModal
        utbetalingId={utbetalingId}
        modalOpen={beregningModalOpen}
        onClose={() => setBeregningModalOpen(false)}
      />
    </VStack>
  );
}
