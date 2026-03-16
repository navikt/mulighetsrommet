import {
  OpprettDelutbetalingerRequest,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinje,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { FileCheckmarkIcon, PencilIcon, PiggybankIcon, TrashFillIcon } from "@navikt/aksel-icons";
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
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import MindreBelopModal from "./MindreBelopModal";
import {
  FieldPath,
  FormProvider,
  useFieldArray,
  useForm,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { GjorOppTilsagnFormCheckbox } from "./GjorOppTilsagnCheckbox";
import { utbetalingTekster } from "./UtbetalingTekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useOpprettDelutbetalinger, useSlettKorreksjon } from "@/api/utbetaling/mutations";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { extractValidationErrors } from "@/utils/Utils";

export interface Props {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
  utbetalingLinjer: UtbetalingLinje[];
}

export function RedigerUtbetalingLinjeView({ utbetaling, handlinger, utbetalingLinjer }: Props) {
  const navigate = useNavigate();
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const [slettKorreksjonModalOpen, setSlettKorreksjonModalOpen] = useState<boolean>(false);

  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  const methods = useForm<OpprettDelutbetalingerRequest>({
    defaultValues: {
      utbetalingId: utbetaling.id,
      delutbetalinger: utbetalingLinjer.map((linje) => ({
        pris: linje.pris,
        id: linje.id,
        tilsagnId: linje.tilsagn.id,
      })),
      begrunnelseMindreBetalt: null,
    },
  });

  const {
    handleSubmit,
    setError,
    register,
    clearErrors,
    getValues,
    control,
    setValue,
    reset,
    formState: { errors },
  } = methods;

  useEffect(() => {
    reset({
      utbetalingId: utbetaling.id,
      delutbetalinger: utbetalingLinjer.map((linje) => ({
        pris: linje.pris,
        id: linje.id,
        tilsagnId: linje.tilsagn.id,
      })),
      begrunnelseMindreBetalt: null,
    });
  }, [utbetaling.id, utbetalingLinjer, reset]);

  const delutbetalinger = useWatch({
    control,
    name: "delutbetalinger",
  });

  const delutbetalingBelop = useWatch({
    control,
    name: delutbetalinger.map((_, index) => `delutbetalinger.${index}.pris.belop` as const),
  });

  const { gjennomforingId, periode, tilskuddstype, beregning } = utbetaling;

  const utbetalesTotalt = {
    valuta: beregning.valuta,
    belop: delutbetalingBelop.reduce((acc, belop) => acc + belop, 0),
  };

  function sendTilAttestering(payload: OpprettDelutbetalingerRequest) {
    clearErrors();

    opprettMutation.mutate(payload, {
      onValidationError: (error: ValidationError) => {
        error.errors.forEach((error) => {
          const fieldPath = jsonPointerToFieldPath(error.pointer);
          const name = (fieldPath || "root") as FieldPath<OpprettDelutbetalingerRequest>;
          setError(name, { type: "custom", message: error.detail });
        });
      },
    });
  }

  const tilsagnsTypeFraTilskudd = tilsagnType(tilskuddstype);

  function opprettEkstraTilsagn() {
    const defaultTilsagn = utbetalingLinjer.length === 1 ? utbetalingLinjer[0].tilsagn : undefined;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnsTypeFraTilskudd}` +
        `&periodeStart=${periode.start}` +
        `&periodeSlutt=${yyyyMMddFormatting(subDuration(periode.slutt, { days: 1 }))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer || ""}`,
    );
  }

  function submitHandler(data: OpprettDelutbetalingerRequest) {
    if (utbetalesTotalt.belop < beregning.belop) {
      setMindreBelopModalOpen(true);
    } else {
      sendTilAttestering(data);
    }
  }

  const aktiveLinjer = utbetalingLinjer.filter((linje) =>
    delutbetalinger.some((d) => linje.id === d.id),
  );

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(submitHandler)}>
        <VStack gap="space-8">
          {!utbetalingLinjer.length && (
            <Alert variant="info">{utbetalingTekster.delutbetaling.alert.ingenTilsagn}</Alert>
          )}
          <HStack align="end">
            <Heading spacing size="medium" level="2">
              {utbetalingTekster.delutbetaling.header}
            </Heading>
            <Spacer />
            <Handlinger>
              {handlinger.includes(UtbetalingHandling.REDIGER) && (
                <ActionMenu.Item
                  icon={<PencilIcon />}
                  onClick={() => navigate("rediger-utbetaling")}
                >
                  Rediger utbetaling
                </ActionMenu.Item>
              )}
              <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
                {utbetalingTekster.delutbetaling.handlinger.opprettTilsagn(tilsagnsTypeFraTilskudd)}
              </ActionMenu.Item>
              <ActionMenu.Item
                icon={<FileCheckmarkIcon />}
                onSelect={() =>
                  setValue(
                    "delutbetalinger",
                    utbetalingLinjer.map((linje) => ({
                      id: linje.id,
                      pris: linje.pris,
                      tilsagnId: linje.tilsagn.id,
                      gjorOppTilsagn: linje.gjorOppTilsagn,
                    })),
                  )
                }
              >
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
            </Handlinger>
          </HStack>

          <UtbetalingLinjeTable
            utbetaling={utbetaling}
            linjer={aktiveLinjer}
            renderRow={(linje: UtbetalingLinje, index: number) => (
              <UtbetalingLinjeRow
                key={`${linje.id}-${linje.status?.type}`}
                gjennomforingId={utbetaling.gjennomforingId}
                linje={linje}
                belopInput={
                  <TextField
                    size="small"
                    style={{ maxWidth: "6rem" }}
                    hideLabel
                    type="text"
                    error={errors.delutbetalinger?.[index]?.pris?.belop?.message}
                    label={utbetalingTekster.delutbetaling.belop.label}
                    {...register(`delutbetalinger.${index}.pris.belop`, {
                      setValueAs: (v: string) => (v === "" ? null : Number(v)),
                      validate: (value: number | null) => {
                        if (!Number.isInteger(value)) return "Beløp må være et heltall";
                        return true;
                      },
                    })}
                  />
                }
                errors={extractValidationErrors(errors)}
                checkboxInput={<GjorOppTilsagnFormCheckbox index={index} />}
                knappeColumn={<FjernUtbetalingLinje index={index} />}
              />
            )}
          />
          {delutbetalinger.length > 0 && (
            <HStack gap="space-8" justify="end">
              <ValideringsfeilOppsummering />
              {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
                <Button size="small" type="submit">
                  {utbetalingTekster.delutbetaling.handlinger.sendTilAttestering}
                </Button>
              )}
            </HStack>
          )}
        </VStack>

        <MindreBelopModal
          open={mindreBelopModalOpen}
          handleClose={() => setMindreBelopModalOpen(false)}
          onConfirm={() => {
            setMindreBelopModalOpen(false);
            const formData = getValues();
            sendTilAttestering(formData);
          }}
          begrunnelseOnChange={(e) => setValue("begrunnelseMindreBetalt", e.target.value)}
          belopUtbetaling={utbetalesTotalt}
          belopInnsendt={beregning}
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

function tilsagnType(tilskuddstype: Tilskuddstype): TilsagnType {
  switch (tilskuddstype) {
    case Tilskuddstype.TILTAK_DRIFTSTILSKUDD:
      return TilsagnType.EKSTRATILSAGN;
    case Tilskuddstype.TILTAK_INVESTERINGER:
      return TilsagnType.INVESTERING;
  }
}

function FjernUtbetalingLinje({ index }: { index: number }) {
  const { control } = useFormContext<OpprettDelutbetalingerRequest>();
  const { remove } = useFieldArray<OpprettDelutbetalingerRequest>({
    control,
    name: "delutbetalinger",
  });

  return (
    <Button
      data-color="neutral"
      size="small"
      variant="secondary"
      type="button"
      onClick={() => remove(index)}
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
        onSuccess: () => navigate("..", { replace: true }),
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
        <HStack gap="space-16">
          <Button type="button" variant="secondary" onClick={onClose}>
            Nei, takk
          </Button>
          <Button
            data-color="danger"
            title="Slett utbetaling"
            variant="primary"
            onClick={slettKorreksjon}
          >
            Ja, jeg vil slette utbetalingen
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
