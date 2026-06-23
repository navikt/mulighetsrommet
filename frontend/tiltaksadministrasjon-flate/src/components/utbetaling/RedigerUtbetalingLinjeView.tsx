import {
  OpprettUtbetalingLinjerRequest,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinjeDto,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { FileCheckmarkIcon, PencilIcon, PiggybankIcon, TrashFillIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  Heading,
  HStack,
  Modal,
  Spacer,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { GjorOppTilsagnFormCheckbox } from "./GjorOppTilsagnCheckbox";
import { utbetalingTekster } from "./UtbetalingTekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useOpprettUtbetalingLinjer, useSlettKorreksjon } from "@/api/utbetaling/mutations";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { extractValidationErrors } from "@/utils/Utils";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";

export interface Props {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
  utbetalingLinjer: UtbetalingLinjeDto[];
}

export function RedigerUtbetalingLinjeView({ utbetaling, handlinger, utbetalingLinjer }: Props) {
  const navigate = useNavigate();
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const [slettKorreksjonModalOpen, setSlettKorreksjonModalOpen] = useState<boolean>(false);

  const opprettMutation = useOpprettUtbetalingLinjer(utbetaling.id);

  const form = useForm<OpprettUtbetalingLinjerRequest>({
    defaultValues: {
      utbetalingId: utbetaling.id,
      utbetalingLinjer: utbetalingLinjer.map((linje) => ({
        pris: linje.pris,
        id: linje.id,
        tilsagnId: linje.tilsagn.id,
      })),
      begrunnelseMindreBetalt: null,
    },
  });

  const {
    handleSubmit,
    register,
    clearErrors,
    getValues,
    control,
    setValue,
    reset,
    formState: { errors },
  } = form;

  useEffect(() => {
    reset({
      utbetalingId: utbetaling.id,
      utbetalingLinjer: utbetalingLinjer.map((linje) => ({
        pris: linje.pris,
        id: linje.id,
        tilsagnId: linje.tilsagn.id,
      })),
      begrunnelseMindreBetalt: null,
    });
  }, [utbetaling.id, utbetalingLinjer, reset]);

  const utbetalingLinjerWatch = useWatch({
    control,
    name: "utbetalingLinjer",
  });

  const utbetalingLinjeBelop = useWatch({
    control,
    name: utbetalingLinjerWatch.map((_, index) => `utbetalingLinjer.${index}.pris.belop` as const),
  });

  const { gjennomforingId, periode, tilskuddstype, beregning } = utbetaling;

  const utbetalesTotalt = {
    valuta: beregning.valuta,
    belop: utbetalingLinjeBelop.reduce((acc: number, belop) => acc + (belop ?? 0), 0),
  };

  function sendTilAttestering(payload: OpprettUtbetalingLinjerRequest) {
    clearErrors();

    opprettMutation.mutate(payload, {
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
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

  function submitHandler(data: OpprettUtbetalingLinjerRequest) {
    if (utbetalesTotalt.belop < beregning.belop) {
      setMindreBelopModalOpen(true);
    } else {
      sendTilAttestering(data);
    }
  }

  const aktiveLinjer = utbetalingLinjer.filter((linje) =>
    utbetalingLinjerWatch.some((d) => linje.id === d.id),
  );

  return (
    <FormProvider {...form}>
      <form
        onSubmit={(e) => {
          clearErrors();
          handleSubmit(submitHandler)(e);
        }}
      >
        <VStack gap="space-8">
          {!utbetalingLinjer.length && (
            <Alert variant="info">{utbetalingTekster.linje.alert.ingenTilsagn}</Alert>
          )}
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
                      label: "Rediger utbetaling",
                      href: "rediger-utbetaling",
                      icon: <PencilIcon />,
                      handling: UtbetalingHandling.REDIGER,
                    },
                    {
                      label:
                        utbetalingTekster.linje.handlinger.opprettTilsagn(tilsagnsTypeFraTilskudd),
                      onClick: opprettEkstraTilsagn,
                      icon: <PiggybankIcon />,
                    },
                    {
                      label: utbetalingTekster.linje.handlinger.hentGodkjenteTilsagn,
                      onClick: () =>
                        setValue(
                          "utbetalingLinjer",
                          utbetalingLinjer.map((linje) => ({
                            id: linje.id,
                            pris: linje.pris,
                            tilsagnId: linje.tilsagn.id,
                            gjorOppTilsagn: linje.gjorOppTilsagn,
                          })),
                        ),
                      icon: <FileCheckmarkIcon />,
                    },
                    {
                      label: "Slett utbetaling",
                      onClick: () => setSlettKorreksjonModalOpen(true),
                      icon: <TrashFillIcon />,
                      handling: UtbetalingHandling.SLETT,
                    },
                  ],
                },
              ]}
            />
          </HStack>

          <UtbetalingLinjeTable
            utbetaling={utbetaling}
            linjer={aktiveLinjer}
            utbetalesTotal={utbetalesTotalt.belop}
            renderRow={(linje: UtbetalingLinjeDto, index: number) => (
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
                    error={errors.utbetalingLinjer?.[index]?.pris?.belop?.message}
                    label={utbetalingTekster.linje.belop.label}
                    {...register(`utbetalingLinjer.${index}.pris.belop`, {
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
                knappeColumn={
                  <FjernUtbetalingLinje
                    onRemove={() => {
                      const current = getValues("utbetalingLinjer");
                      setValue(
                        "utbetalingLinjer",
                        current.filter((_, i) => i !== index),
                      );
                    }}
                  />
                }
              />
            )}
          />
          {utbetalingLinjerWatch.length > 0 && (
            <HStack gap="space-8" justify="end">
              <ValideringsfeilOppsummering />
              {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
                <Button size="small" type="submit">
                  {utbetalingTekster.linje.handlinger.sendTilAttestering}
                </Button>
              )}
            </HStack>
          )}
        </VStack>

        <VarselModal
          open={mindreBelopModalOpen}
          handleClose={() => setMindreBelopModalOpen(false)}
          primaryButton={
            <Button
              variant="primary"
              onClick={() => {
                setMindreBelopModalOpen(false);
                const formData = getValues();
                sendTilAttestering(formData);
              }}
            >
              Ja, send til attestering
            </Button>
          }
          headingText="Beløp til utbetaling er mindre enn innsendt beløp"
          headingIconType="warning"
          body={
            <VStack gap="space-16">
              <BodyShort>
                Beløpet du er i ferd med å sende til attestering er mindre enn beløpet på
                utbetalingen. Er du sikker på at du vil fortsette?
              </BodyShort>
              <VStack>
                <BodyShort weight="semibold">
                  Beløp til attestering: {formaterValutaBelop(utbetalesTotalt)}
                </BodyShort>
                <BodyShort weight="semibold">
                  Innsendt beløp: {formaterValutaBelop(beregning)}
                </BodyShort>
              </VStack>
              <BodyLong color="contrast">
                Husk at for tiltakene Oppfølging, Avklaring, ARR og Digitalt jobbsøkerkurs skal
                arrangør alltid få utbetalt for gjennomført aktivitet. Det gjelder også for
                eventuelle andre tiltak hvor avtalt pris er basert på gjennomført aktivitet.
              </BodyLong>
              <Textarea
                label="Begrunnelse"
                onChange={(e) => setValue("begrunnelseMindreBetalt", e.target.value)}
                description="Oppgi begrunnelse for beløpet som utbetales. Begrunnelsen vil kun være synlig for Nav."
              />
            </VStack>
          }
          secondaryButton
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
    case Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD:
      return TilsagnType.EKSTRATILSAGN;
    case Tilskuddstype.TILTAK_INVESTERINGER:
      return TilsagnType.INVESTERING;
  }
}

function FjernUtbetalingLinje({ onRemove }: { onRemove: () => void }) {
  return (
    <Button data-color="neutral" size="small" variant="secondary" type="button" onClick={onRemove}>
      {utbetalingTekster.linje.handlinger.fjern}
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
