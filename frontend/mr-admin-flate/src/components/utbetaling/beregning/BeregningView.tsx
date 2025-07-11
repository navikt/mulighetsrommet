import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { UtbetalingBeregningDto } from "@mr/api-client-v2";
import { useState } from "react";
import Beregning from "./Beregning";
import { BeregningModal } from "./BeregningModal";

interface Props {
  beregning: UtbetalingBeregningDto;
  utbetalingId: string;
}

export default function BeregningView({ beregning, utbetalingId }: Props) {
  const [beregningModalOpen, setBeregningModalOpen] = useState<boolean>(false);

  return (
    <VStack gap="2">
      <div className={`max-h-[30rem] overflow-y-scroll`}>
        <Heading size="small">{beregning.heading}</Heading>
        <Beregning beregning={beregning} />
      </div>
      <HStack justify="start" align="start">
        {beregning.deltakerTableData.rows.length > 0 && (
          <Button variant="secondary" size="small" onClick={() => setBeregningModalOpen(true)}>
            Filtreringshjelp
          </Button>
        )}
      </HStack>
      <BeregningModal
        utbetalingId={utbetalingId}
        modalOpen={beregningModalOpen}
        onClose={() => setBeregningModalOpen(false)}
      />
    </VStack>
  );
}
