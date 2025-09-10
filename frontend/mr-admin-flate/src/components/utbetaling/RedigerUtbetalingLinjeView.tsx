import { FieldError, ValidationError as LegacyValidationError } from "@mr/api-client-v2";
import {
  OpprettDelutbetalingerRequest,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinje,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { FileCheckmarkIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { ActionMenu, Alert, Button, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import MindreBelopModal from "./MindreBelopModal";
import { FormProvider, useFieldArray, useForm } from "react-hook-form";
import { isBesluttet } from "@/utils/totrinnskontroll";
import { getChangeSet, RedigerUtbetalingLinjeFormValues, toDelutbetaling } from "./helpers";
import { GjorOppTilsagnFormCheckbox } from "./GjorOppTilsagnCheckbox";
import { UtbetalingBelopInput } from "./UtbetalingBelopInput";
import { utbetalingTekster } from "./UtbetalingTekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export interface Props {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
  utbetalingLinjer: UtbetalingLinje[];
  oppdaterLinjer: () => Promise<void>;
  reloadLinjer?: boolean;
}

export function RedigerUtbetalingLinjeView({
  utbetaling,
  handlinger,
  utbetalingLinjer: apiLinjer,
  oppdaterLinjer,
  reloadLinjer,
}: Props) {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const navigate = useNavigate();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  function sendTilAttestering(payload: OpprettDelutbetalingerRequest) {
    setErrors([]);

    opprettMutation.mutate(payload, {
      onSuccess: oppdaterLinjer,
      onValidationError: (error: ValidationError | LegacyValidationError) => {
        setErrors(error.errors);
      },
    });
  }

  const form = useForm<RedigerUtbetalingLinjeFormValues>({
    defaultValues: { formLinjer: apiLinjer },
    mode: "onSubmit",
  });
  const { append, update } = useFieldArray<RedigerUtbetalingLinjeFormValues>({
    name: "formLinjer",
    control: form.control,
  });
  const formLinjer = form.watch("formLinjer");

  useEffect(() => {
    if (reloadLinjer) {
      const { updatedLinjer, newLinjer } = getChangeSet(formLinjer, apiLinjer);
      updatedLinjer.forEach(({ index, linje }) => {
        update(index, linje);
      });
      newLinjer.forEach((linje) => {
        append(linje);
      });
    }
  }, [reloadLinjer, apiLinjer, formLinjer, append, update]);

  const tilsagnsTypeFraTilskudd = tilsagnType(utbetaling.tilskuddstype);

  function opprettEkstraTilsagn() {
    const defaultTilsagn = apiLinjer.length === 1 ? apiLinjer[0].tilsagn : undefined;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnsTypeFraTilskudd}` +
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 }))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer || ""}`,
    );
  }

  function submitHandler(data: RedigerUtbetalingLinjeFormValues) {
    if (utbetalesTotal(data.formLinjer) < utbetaling.belop) {
      setMindreBelopModalOpen(true);
    } else {
      sendTilAttestering({
        utbetalingId: utbetaling.id,
        delutbetalinger: data.formLinjer.map(toDelutbetaling),
        begrunnelseMindreBetalt,
      });
    }
  }

  function openRow(linje: UtbetalingLinje): boolean {
    const hasNonBelopErrors = errors.filter((e) => !e.pointer.includes("belop"));
    return hasNonBelopErrors.length > 0 || isBesluttet(linje.opprettelse);
  }

  return (
    <FormProvider {...form}>
      <form onSubmit={form.handleSubmit(submitHandler)}>
        <VStack gap="2">
          {!formLinjer.length && (
            <Alert variant="info">{utbetalingTekster.delutbetaling.alert.ingenTilsagn}</Alert>
          )}
          <HStack align="end">
            <Heading spacing size="medium" level="2">
              {utbetalingTekster.delutbetaling.header}
            </Heading>
            <Spacer />
            <ActionMenu>
              <ActionMenu.Trigger>
                <Button variant="secondary" size="small">
                  {utbetalingTekster.delutbetaling.handlinger.button.label}
                </Button>
              </ActionMenu.Trigger>
              <ActionMenu.Content>
                <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
                  {utbetalingTekster.delutbetaling.handlinger.opprettTilsagn(
                    tilsagnsTypeFraTilskudd,
                  )}
                </ActionMenu.Item>
                <ActionMenu.Item icon={<FileCheckmarkIcon />} onSelect={oppdaterLinjer}>
                  {utbetalingTekster.delutbetaling.handlinger.hentGodkjenteTilsagn}
                </ActionMenu.Item>
              </ActionMenu.Content>
            </ActionMenu>
          </HStack>

          <UtbetalingLinjeTable
            utbetaling={utbetaling}
            linjer={formLinjer}
            renderRow={(linje: UtbetalingLinje, index: number) => (
              <UtbetalingLinjeRow
                key={`${linje.id}-${linje.status?.type}`}
                gjennomforingId={gjennomforingId}
                linje={linje}
                textInput={<UtbetalingBelopInput type="form" index={index} />}
                checkboxInput={<GjorOppTilsagnFormCheckbox index={index} />}
                knappeColumn={<FjernUtbetalingLinje index={index} />}
                grayBackground
                errors={errors.filter(
                  (f) => f.pointer.startsWith(`/${index}`) || f.pointer.includes("totalbelop"),
                )}
                rowOpen={openRow(linje)}
              />
            )}
          />
        </VStack>

        <VStack gap="2" className="mt-2">
          {!!formLinjer.length && (
            <HStack justify="end">
              {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
                <Button size="small" type="submit">
                  {utbetalingTekster.delutbetaling.handlinger.sendTilAttestering}
                </Button>
              )}
            </HStack>
          )}
          <VStack gap="2" align="end">
            {errors.map((error) => (
              <Alert variant="error" size="small">
                {error.detail}
              </Alert>
            ))}
          </VStack>
        </VStack>

        <MindreBelopModal
          open={mindreBelopModalOpen}
          handleClose={() => setMindreBelopModalOpen(false)}
          onConfirm={() => {
            setMindreBelopModalOpen(false);
            const formLinjer = form.getValues("formLinjer");

            sendTilAttestering({
              utbetalingId: utbetaling.id,
              delutbetalinger: formLinjer.map(toDelutbetaling),
              begrunnelseMindreBetalt,
            });
          }}
          begrunnelseOnChange={(e: any) => setBegrunnelseMindreBetalt(e.target.value)}
          belopUtbetaling={utbetalesTotal(formLinjer)}
          belopInnsendt={utbetaling.belop}
        />
      </form>
    </FormProvider>
  );
}

function utbetalesTotal(formLinjer: UtbetalingLinje[]): number {
  return formLinjer.reduce((acc: number, d: UtbetalingLinje) => acc + d.belop, 0);
}

function tilsagnType(tilskuddstype: Tilskuddstype): TilsagnType {
  switch (tilskuddstype) {
    case Tilskuddstype.TILTAK_DRIFTSTILSKUDD:
      return TilsagnType.EKSTRATILSAGN;
    case Tilskuddstype.TILTAK_INVESTERINGER:
      return TilsagnType.INVESTERING;
  }
}

function FjernUtbetalingLinje({ index }: { index: number }) {
  const { remove } = useFieldArray<RedigerUtbetalingLinjeFormValues>({ name: "formLinjer" });
  return (
    <Button
      size="small"
      variant="secondary-neutral"
      type="button"
      onClick={() => {
        remove(index);
      }}
    >
      {utbetalingTekster.delutbetaling.handlinger.fjern}
    </Button>
  );
}
