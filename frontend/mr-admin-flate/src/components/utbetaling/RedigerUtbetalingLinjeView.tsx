import { FieldError, ValidationError } from "@mr/api-client-v2";
import {
  DelutbetalingRequest,
  OpprettDelutbetalingerRequest,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinje,
} from "@tiltaksadministrasjon/api-client";
import { FileCheckmarkIcon, PiggybankIcon } from "@navikt/aksel-icons";
import {
  ActionMenu,
  Alert,
  Button,
  Checkbox,
  Heading,
  HelpText,
  HStack,
  Spacer,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import MindreBelopModal from "./MindreBelopModal";
import { FormProvider, useFieldArray, useForm, useFormContext } from "react-hook-form";
import { isBesluttet } from "@/utils/totrinnskontroll";

type FormValues = {
  formLinjer: UtbetalingLinje[];
};

type UpdatedLinje = { index: number; linje: UtbetalingLinje };

function getUpdatedLinjer(
  formList: UtbetalingLinje[],
  apiLinjer: UtbetalingLinje[],
): UpdatedLinje[] {
  return formList.flatMap((linje, index) => {
    const apiLinje = apiLinjer.find(({ id }) => id === linje.id);

    if (!apiLinje || apiLinje.status == linje.status) {
      return [];
    } else {
      return [
        { index, linje: { ...apiLinje, belop: linje.belop, gjorOppTilsagn: linje.gjorOppTilsagn } },
      ];
    }
  });
}
function getChangeSet(
  formList: UtbetalingLinje[],
  apiLinjer: UtbetalingLinje[],
): { updatedLinjer: UpdatedLinje[]; newLinjer: UtbetalingLinje[] } {
  const updatedLinjer = getUpdatedLinjer(formList, apiLinjer);
  const newLinjer = apiLinjer.filter((apiLinje) => !formList.some(({ id }) => id === apiLinje.id));
  return { updatedLinjer, newLinjer };
}

function toDelutbetaling(linje: UtbetalingLinje): DelutbetalingRequest {
  return {
    id: linje.id,
    tilsagnId: linje.tilsagn.id,
    belop: linje.belop,
    gjorOppTilsagn: linje.gjorOppTilsagn,
  };
}

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
  const { gjennomforingId } = useParams();
  const navigate = useNavigate();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  function sendTilGodkjenning(payload: OpprettDelutbetalingerRequest) {
    setErrors([]);

    opprettMutation.mutate(payload, {
      onSuccess: oppdaterLinjer,
      onValidationError: (error: ValidationError) => {
        setErrors(error.errors);
      },
    });
  }

  const form = useForm<FormValues>({
    defaultValues: { formLinjer: apiLinjer },
    mode: "onSubmit",
  });
  const { append, update } = useFieldArray<FormValues>({
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

  function utbetalesTotal(): number {
    return formLinjer.reduce((acc: number, d: UtbetalingLinje) => acc + d.belop, 0);
  }

  function submitHandler(data?: FormValues) {
    if (utbetalesTotal() < utbetaling.belop) {
      setMindreBelopModalOpen(true);
    } else {
      const formLinjer = data?.formLinjer.length ? data.formLinjer : form.getValues("formLinjer");

      sendTilGodkjenning({
        utbetalingId: utbetaling.id,
        delutbetalinger: formLinjer.map(toDelutbetaling),
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
        {!formLinjer.length && (
          <Alert variant="info">Det finnes ingen godkjente tilsagn for utbetalingsperioden</Alert>
        )}
        <VStack>
          <HStack align="end">
            <Heading spacing size="medium" level="2">
              Utbetalingslinjer
            </Heading>
            <Spacer />
            <ActionMenu>
              <ActionMenu.Trigger>
                <Button variant="secondary" size="small">
                  Handlinger
                </Button>
              </ActionMenu.Trigger>
              <ActionMenu.Content>
                <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
                  Opprett {avtaletekster.tilsagn.type(tilsagnsTypeFraTilskudd).toLowerCase()}
                </ActionMenu.Item>
                <ActionMenu.Item icon={<FileCheckmarkIcon />} onSelect={oppdaterLinjer}>
                  Hent godkjente tilsagn
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
                linje={linje}
                textInput={<UtbetalingBelopInput index={index} />}
                checkboxInput={<GjorOppTilsagnCheckbox index={index} />}
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
          <HStack justify="end">
            {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
              <Button size="small" type="submit">
                Send til attestering
              </Button>
            )}
          </HStack>
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

            sendTilGodkjenning({
              utbetalingId: utbetaling.id,
              delutbetalinger: formLinjer.map(toDelutbetaling),
              begrunnelseMindreBetalt,
            });
          }}
          begrunnelseOnChange={(e: any) => setBegrunnelseMindreBetalt(e.target.value)}
          belopUtbetaling={utbetalesTotal()}
          belopInnsendt={utbetaling.belop}
        />
      </form>
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
  const { remove } = useFieldArray<FormValues>({ name: "formLinjer" });
  return (
    <Button
      size="small"
      variant="secondary-neutral"
      type="button"
      onClick={() => {
        remove(index);
      }}
    >
      Fjern
    </Button>
  );
}

function UtbetalingBelopInput({ index }: { index: number }) {
  const { register } = useFormContext<FormValues>();
  const options = {};
  return (
    <TextField
      size="small"
      style={{ maxWidth: "6rem" }}
      label="Utbetales"
      hideLabel
      inputMode="numeric"
      {...register(`formLinjer.${index}.belop`, options)}
    />
  );
}

function GjorOppTilsagnCheckbox({ index }: { index: number }) {
  const { register } = useFormContext<FormValues>();
  const options = {};
  return (
    <HStack gap="2">
      <Checkbox hideLabel {...register(`formLinjer.${index}.gjorOppTilsagn`, options)}>
        Gjør opp tilsagn
      </Checkbox>
      <HelpText>
        Hvis du huker av for å gjøre opp tilsagnet, betyr det at det ikke kan gjøres flere
        utbetalinger på tilsagnet etter at denne utbetalingen er attestert
      </HelpText>
    </HStack>
  );
}
