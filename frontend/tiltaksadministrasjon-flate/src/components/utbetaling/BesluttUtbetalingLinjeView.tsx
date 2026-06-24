import { useAttesterUtbetalingLinje, useReturnerUtbetalingLinje } from "@/api/utbetaling/mutations";
import {
  AarsakerOgForklaringRequestUtbetalingLinjeReturnertAarsak,
  UtbetalingLinjeReturnertAarsak,
  FieldError,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinjeHandling,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { BodyShort, Button, Heading, HStack, Spacer, TextField, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { useUtbetalingsLinjer } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { utbetalingTekster } from "./UtbetalingTekster";
import { GjorOppTilsagnCheckbox } from "./GjorOppTilsagnCheckbox";
import { PlusCircleIcon } from "@navikt/aksel-icons";
import { OpprettKorreksjonModal } from "@/components/utbetaling/OpprettKorreksjonModal";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";

export interface Props {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
}

export function BesluttUtbetalingLinjeView({ utbetaling, handlinger }: Props) {
  const { data: linjer } = useUtbetalingsLinjer(utbetaling.id);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const [attesterModalOpenForLinjeId, setAttesterModalOpenForLinjeId] = useState<string | null>(
    null,
  );
  const [opprettKorreksjonModalOpen, setOpprettKorreksjonModalOpen] = useState<boolean>(false);
  const [errors, setErrors] = useState<FieldError[]>([]);
  const attesterUtbetalingLinjeMutation = useAttesterUtbetalingLinje();
  const returnerUtbetalingLinjeMutation = useReturnerUtbetalingLinje();

  function attesterUtbetalingLinje(id: string) {
    attesterUtbetalingLinjeMutation.mutate(
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
    body: AarsakerOgForklaringRequestUtbetalingLinjeReturnertAarsak,
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
        <Spacer />
        <Handlinger
          handlinger={handlinger}
          grupper={[
            {
              items: [
                {
                  label: "Opprett korreksjon",
                  onClick: () => setOpprettKorreksjonModalOpen(true),
                  icon: <PlusCircleIcon />,
                  handling: UtbetalingHandling.OPPRETT_KORREKSJON,
                },
              ],
            },
          ]}
        />
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
                  {linje.handlinger.includes(UtbetalingLinjeHandling.ATTESTER) && (
                    <Button
                      key={`attester-knapp-${linje.id}`}
                      size="small"
                      type="button"
                      onClick={() => setAttesterModalOpenForLinjeId(linje.id)}
                    >
                      {utbetalingTekster.linje.handlinger.attester}
                    </Button>
                  )}
                  <AarsakerOgForklaringModal<UtbetalingLinjeReturnertAarsak>
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
                      attesterModalOpenForLinjeId !== null &&
                      attesterModalOpenForLinjeId === linje.id
                    }
                    handleClose={() => {
                      setAttesterModalOpenForLinjeId(null);
                    }}
                    headingText="Attester utbetaling"
                    headingIconType="info"
                    body={
                      <BodyShort>
                        Du er i ferd med å attestere utbetalingsbeløp{" "}
                        {formaterValutaBelop(linje.pris)} for kostnadssted{" "}
                        {linje.tilsagn.kostnadssted.navn}. Er du sikker?
                      </BodyShort>
                    }
                    secondaryButton
                    primaryButton={
                      <Button
                        variant="primary"
                        onClick={() => {
                          setAttesterModalOpenForLinjeId(null);
                          attesterUtbetalingLinje(linje.id);
                        }}
                      >
                        Ja, attester beløp
                      </Button>
                    }
                  />
                </HStack>
              }
            />
          );
        }}
      />
      <OpprettKorreksjonModal
        utbetaling={utbetaling}
        open={opprettKorreksjonModalOpen}
        close={() => setOpprettKorreksjonModalOpen(false)}
      />
    </VStack>
  );
}
