import { useGodkjennUtbetalingLinje, useReturnerUtbetalingLinje } from "@/api/utbetaling/mutations";
import {
  AarsakerOgBegrunnelseRequestUtbetalingLinjeReturnertAarsak,
  UtbetalingLinjeReturnertAarsak,
  FieldError,
  UtbetalingDto,
  UtbetalingLinjeHandling,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { BodyShort, Button, Heading, HStack, TextField, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { AarsakerOgBegrunnelseModal } from "../modal/AarsakerOgBegrunnelseModal";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { useUtbetalingsLinjer } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { utbetalingTekster } from "./UtbetalingTekster";
import { GjorOppTilsagnCheckbox } from "./GjorOppTilsagnCheckbox";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";

export interface Props {
  utbetaling: UtbetalingDto;
}

export function BesluttUtbetalingLinjeView({ utbetaling }: Props) {
  const { data: linjer } = useUtbetalingsLinjer(utbetaling.id);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const [godkjennModalOpenForLinjeId, setGodkjennModalOpenForLinjeId] = useState<string | null>(
    null,
  );
  const [errors, setErrors] = useState<FieldError[]>([]);
  const godkjennUtbetalingLinjeMutation = useGodkjennUtbetalingLinje();
  const returnerUtbetalingLinjeMutation = useReturnerUtbetalingLinje();

  function godkjennUtbetalingLinje(id: string) {
    godkjennUtbetalingLinjeMutation.mutate(
      { id },
      {
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
      },
    );
  }

  function returnerUtbetalingLinje(
    id: string,
    body: AarsakerOgBegrunnelseRequestUtbetalingLinjeReturnertAarsak,
  ) {
    returnerUtbetalingLinjeMutation.mutate(
      { id, body },
      {
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
        onSuccess: () => {
          setAvvisModalOpen(false);
        },
      },
    );
  }

  const returnerAarsakValg = [
    UtbetalingLinjeReturnertAarsak.FEIL_BELOP,
    UtbetalingLinjeReturnertAarsak.ANNET,
  ].map((val) => {
    return {
      value: val,
      label: utbetalingTekster.linje.aarsak.fraRetunertAarsak(val),
    };
  });

  return (
    <VStack gap="space-8">
      <HStack align="end">
        <Heading spacing size="medium" level="2">
          {utbetalingTekster.linje.header}
        </Heading>
      </HStack>
      <UtbetalingLinjeTable
        linjer={linjer.filter((l) => l.status !== null)}
        utbetaling={utbetaling}
        renderRow={(linje) => {
          return (
            <UtbetalingLinjeRow
              key={`${linje.id}-${linje.status?.type}`}
              gjennomforingId={utbetaling.gjennomforingId}
              linje={linje}
              checkboxInput={<GjorOppTilsagnCheckbox linje={linje} />}
              belopInput={
                <TextField
                  readOnly
                  value={linje.pris.belop}
                  size="small"
                  style={{ maxWidth: "6rem" }}
                  hideLabel
                  type="number"
                  label={utbetalingTekster.linje.belop.label}
                />
              }
              knappeColumn={
                <HStack gap="space-16">
                  {linje.handlinger.includes(UtbetalingLinjeHandling.RETURNER) && (
                    <Button
                      variant="secondary"
                      size="small"
                      type="button"
                      onClick={() => setAvvisModalOpen(true)}
                    >
                      {utbetalingTekster.linje.handlinger.returner}
                    </Button>
                  )}
                  {linje.handlinger.includes(UtbetalingLinjeHandling.GODKJENN) && (
                    <Button
                      key={`godkjenn-knapp-${linje.id}`}
                      size="small"
                      type="button"
                      onClick={() => setGodkjennModalOpenForLinjeId(linje.id)}
                    >
                      {utbetalingTekster.linje.handlinger.godkjenn}
                    </Button>
                  )}
                  <AarsakerOgBegrunnelseModal<UtbetalingLinjeReturnertAarsak>
                    header={utbetalingTekster.linje.aarsak.modal.header}
                    ingress={<BodyShort>{utbetalingTekster.linje.aarsak.modal.ingress}</BodyShort>}
                    open={avvisModalOpen}
                    buttonLabel={utbetalingTekster.linje.aarsak.modal.button.label}
                    errors={errors}
                    aarsaker={returnerAarsakValg}
                    onClose={() => {
                      setAvvisModalOpen(false);
                      setErrors([]);
                    }}
                    onConfirm={(request) => {
                      returnerUtbetalingLinje(linje.id, request);
                    }}
                  />
                  <VarselModal
                    open={
                      godkjennModalOpenForLinjeId !== null &&
                      godkjennModalOpenForLinjeId === linje.id
                    }
                    handleClose={() => {
                      setGodkjennModalOpenForLinjeId(null);
                    }}
                    headingText="Godkjennutbetaling"
                    headingIconType="info"
                    body={
                      <BodyShort>
                        Du er i ferd med å godkjenne utbetalingsbeløp{" "}
                        {formaterValutaBelop(linje.pris)} for kostnadssted{" "}
                        {linje.tilsagn.kostnadssted.navn}. Er du sikker?
                      </BodyShort>
                    }
                    secondaryButton
                    primaryButton={
                      <Button
                        variant="primary"
                        onClick={() => {
                          setGodkjennModalOpenForLinjeId(null);
                          godkjennUtbetalingLinje(linje.id);
                        }}
                      >
                        Ja, godkjenn beløp
                      </Button>
                    }
                  />
                </HStack>
              }
            />
          );
        }}
      />
    </VStack>
  );
}
