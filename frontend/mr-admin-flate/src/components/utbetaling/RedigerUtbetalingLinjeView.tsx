import {
  FieldError,
  OpprettDelutbetalingerRequest,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinje,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { FileCheckmarkIcon, PiggybankIcon, TrashFillIcon } from "@navikt/aksel-icons";
import {
  ActionMenu,
  Alert,
  BodyShort,
  Button,
  Heading,
  HStack,
  Modal,
  Spacer,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import MindreBelopModal from "./MindreBelopModal";
import { FormProvider, useFieldArray, useForm } from "react-hook-form";
import { isBesluttet } from "@/utils/totrinnskontroll";
import { RedigerUtbetalingLinjeFormValues, toDelutbetaling } from "./helpers";
import { GjorOppTilsagnFormCheckbox } from "./GjorOppTilsagnCheckbox";
import { utbetalingTekster } from "./UtbetalingTekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useOpprettDelutbetalinger, useSlettKorreksjon } from "@/api/utbetaling/mutations";

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
}: Props) {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const navigate = useNavigate();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);
  const [slettKorreksjonModalOpen, setSlettKorreksjonModalOpen] = useState<boolean>(false);

  function sendTilAttestering(payload: OpprettDelutbetalingerRequest) {
    setErrors([]);

    opprettMutation.mutate(payload, {
      onSuccess: oppdaterLinjer,
      onValidationError: (error: ValidationError) => {
        setErrors(error.errors);
      },
    });
  }

  const form = useForm<RedigerUtbetalingLinjeFormValues>({
    values: { formLinjer: apiLinjer },
    mode: "onSubmit",
  });
  const formLinjer = form.watch("formLinjer");

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
                {handlinger.includes(UtbetalingHandling.SLETT) && (
                  <ActionMenu.Item
                    icon={<TrashFillIcon />}
                    onSelect={() => setSlettKorreksjonModalOpen(true)}
                  >
                    Slett utbetaling
                  </ActionMenu.Item>
                )}
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
                belopInput={
                  <TextField
                    size="small"
                    style={{ maxWidth: "6rem" }}
                    hideLabel
                    type="number"
                    label={utbetalingTekster.delutbetaling.belop.label}
                    {...form.register(`formLinjer.${index}.belop`, {
                      setValueAs: (v) => (v === "" ? null : Number(v)),
                    })}
                  />
                }
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
      <SlettUtbetalingModal
        utbetalingId={utbetaling.id}
        open={slettKorreksjonModalOpen}
        onClose={() => {
          setSlettKorreksjonModalOpen(false);
        }}
      />
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

function SlettUtbetalingModal({
  utbetalingId,
  open,
  onClose,
}: {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}) {
  const navigate = useNavigate();
  const slettKorreksjonMutation = useSlettKorreksjon();

  function slettKorreksjon() {
    slettKorreksjonMutation.mutate(
      { id: utbetalingId },
      {
        onSuccess: () => navigate(-1),
      },
    );
  }

  return (
    <Modal onClose={onClose} closeOnBackdropClick aria-label="modal" open={open}>
      <Modal.Header closeButton={false}>
        <Heading align="start" size="medium">
          Slett utbetaling
        </Heading>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          Du er i ferd med å slette en korrigeringsutbetaling. Dette vil fjerne den valgte
          ubetalingen fra løsningen. Er du sikker på at du vil fortsette?
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="4">
          <Button type="button" variant="secondary" onClick={onClose}>
            Nei, takk
          </Button>
          <Button title="Slett utbetaling" variant="danger" onClick={slettKorreksjon}>
            Ja, jeg vil slette utbetalingen
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
