import {
  BesluttDelutbetalingRequest,
  Besluttelse,
  DelutbetalingStatus,
  ProblemDetail,
  UtbetalingDto,
  UtbetalingLinje,
} from "@mr/api-client-v2";
import { Button, Heading, HStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
}

export function BesluttUtbetalingLinjeView({ linjer, utbetaling }: Props) {
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const queryClient = useQueryClient();

  const besluttMutation = useBesluttDelutbetaling();

  function beslutt(id: string, body: BesluttDelutbetalingRequest) {
    besluttMutation.mutate(
      { id, body },
      {
        onSuccess: () => {
          return queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
        },
        onError: (error: ProblemDetail) => {
          throw error;
        },
      },
    );
  }

  return (
    <>
      <Heading spacing size="medium">
        Utbetalingslinjer
      </Heading>
      <UtbetalingLinjeTable
        linjer={linjer}
        utbetaling={utbetaling}
        renderRow={(linje) => {
          return (
            <UtbetalingLinjeRow
              readOnly
              key={linje.id}
              linje={linje}
              grayBackground
              knappeColumn={
                <>
                  {linje?.status === DelutbetalingStatus.TIL_GODKJENNING &&
                    linje.opprettelse?.kanBesluttes && (
                      <HStack gap="4">
                        <Button
                          size="small"
                          type="button"
                          onClick={() =>
                            beslutt(linje.id, {
                              besluttelse: Besluttelse.GODKJENT,
                            })
                          }
                        >
                          Godkjenn
                        </Button>
                        <Button
                          variant="secondary"
                          size="small"
                          type="button"
                          onClick={() => setAvvisModalOpen(true)}
                        >
                          Send i retur
                        </Button>
                        <AarsakerOgForklaringModal
                          open={avvisModalOpen}
                          header="Send i retur med forklaring"
                          buttonLabel="Send i retur"
                          aarsaker={[
                            { value: "FEIL_BELOP", label: "Feil beløp" },
                            { value: "FEIL_ANNET", label: "Annet" },
                          ]}
                          onClose={() => setAvvisModalOpen(false)}
                          onConfirm={({ aarsaker, forklaring }) => {
                            beslutt(linje.id, {
                              besluttelse: Besluttelse.AVVIST,
                              aarsaker,
                              forklaring: forklaring ?? null,
                            });
                            setAvvisModalOpen(false);
                          }}
                        />
                      </HStack>
                    )}
                </>
              }
            />
          );
        }}
      />
    </>
  );
}
