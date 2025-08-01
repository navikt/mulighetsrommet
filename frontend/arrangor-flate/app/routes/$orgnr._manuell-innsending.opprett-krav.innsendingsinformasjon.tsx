import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  DatePicker,
  ErrorSummary,
  GuidePanel,
  Heading,
  HStack,
  Label,
  Link,
  Radio,
  RadioGroup,
  TextField,
  UNSAFE_Combobox,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import {
  ArrangorflateGjennomforing,
  ArrangorflateService,
  ArrangorflateTilsagn,
  FieldError,
  TilsagnStatus,
  TilsagnType,
  Tilskuddstype,
} from "api-client";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  ActionFunctionArgs,
  Form,
  Link as ReactRouterLink,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useLoaderData,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { errorAt, problemDetailResponse } from "~/utils/validering";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { commitSession, destroySession, getSession } from "~/sessions.server";
import { formaterDato, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { subtractDays } from "~/utils/date";
import { pathByOrgnr } from "~/utils/navigation";

type LoaderData = {
  gjennomforinger: ArrangorflateGjennomforing[];
  tilsagn: ArrangorflateTilsagn[];
  orgnr: string;
  arrangor: string;
  sessionGjennomforingId?: string;
  sessionTilsagnId?: string;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 1 av 3: Innsendingsinformasjon - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Fyll ut grunnleggende innsendingsinformasjon for å opprette krav om utbetaling",
    },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  const session = await getSession(request.headers.get("Cookie"));

  let sessionGjennomforingId: string | undefined;
  let sessionTilsagnId: string | undefined;
  let sessionPeriodeStart: string | undefined;
  let sessionPeriodeSlutt: string | undefined;
  if (
    session.get("orgnr") === orgnr &&
    session.get("tilskuddstype") === Tilskuddstype.TILTAK_INVESTERINGER
  ) {
    sessionGjennomforingId = session.get("gjennomforingId");
    sessionTilsagnId = session.get("tilsagnId");
    sessionPeriodeStart = session.get("periodeStart");
    sessionPeriodeSlutt = session.get("periodeSlutt");
  }
  const [
    { data: gjennomforinger, error: gjennomforingerError },
    { data: tilsagn, error: tilsagnError },
    { data: arrangortilganger, error: arrangorError },
  ] = await Promise.all([
    ArrangorflateService.getArrangorflateGjennomforinger({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getAllArrangorflateTilsagn({
      path: { orgnr },
      query: { typer: [TilsagnType.INVESTERING], statuser: [TilsagnStatus.GODKJENT] },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil({
      headers: await apiHeaders(request),
    }),
  ]);

  if (gjennomforingerError) {
    throw problemDetailResponse(gjennomforingerError);
  }
  if (tilsagnError || !tilsagn) {
    throw problemDetailResponse(tilsagnError);
  }
  if (!arrangortilganger || arrangorError) {
    throw problemDetailResponse(arrangorError);
  }

  const arrangor = arrangortilganger.find((a) => a.organisasjonsnummer === orgnr)?.navn;
  if (!arrangor) throw new Error("Finner ikke arrangør");

  return {
    orgnr,
    arrangor,
    gjennomforinger,
    tilsagn,
    sessionGjennomforingId,
    sessionTilsagnId,
    sessionPeriodeStart,
    sessionPeriodeSlutt,
  };
};

export async function action({ request }: ActionFunctionArgs) {
  const session = await getSession(request.headers.get("Cookie"));
  const errors: FieldError[] = [];

  const formData = await request.formData();
  const intent = formData.get("intent");
  const orgnr = formData.get("orgnr")?.toString();
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }

  if (intent === "cancel") {
    return redirect(pathByOrgnr(orgnr).utbetalinger, {
      headers: {
        "Set-Cookie": await destroySession(session),
      },
    });
  }

  const periodeStart = formData.get("periodeStart")?.toString();
  const periodeSlutt = formData.get("periodeSlutt")?.toString();
  const gjennomforingId = formData.get("gjennomforingId")?.toString();
  const tilsagnId = formData.get("tilsagnId")?.toString();

  if (!gjennomforingId) {
    errors.push({
      pointer: "/gjennomforingId",
      detail: "Du må fylle ut gjennomføring",
    });
  } else if (!tilsagnId) {
    errors.push({
      pointer: "/tilsagnId",
      detail: "Kan ikke opprette utbetalingskrav uten gyldig tilsagn",
    });
  } else if (!periodeStart) {
    errors.push({
      pointer: "/periodeStart",
      detail: "Du må fylle ut fra dato",
    });
  } else if (!periodeSlutt) {
    errors.push({
      pointer: "/periodeSlutt",
      detail: "Du må fylle ut til dato",
    });
  }

  if (errors.length > 0) {
    return { errors };
  } else {
    session.set("tilskuddstype", Tilskuddstype.TILTAK_INVESTERINGER);
    session.set("orgnr", orgnr);
    session.set("gjennomforingId", gjennomforingId);
    session.set("tilsagnId", tilsagnId);
    session.set("periodeStart", yyyyMMddFormatting(periodeStart));
    session.set("periodeSlutt", yyyyMMddFormatting(periodeSlutt));
    return redirect(pathByOrgnr(orgnr).opprettKravUtbetaling, {
      headers: {
        "Set-Cookie": await commitSession(session),
      },
    });
  }
}

interface ActionData {
  errors: FieldError[];
}

export default function OpprettKravInnsendingsinformasjon() {
  const {
    orgnr,
    arrangor,
    gjennomforinger,
    tilsagn,
    sessionGjennomforingId,
    sessionTilsagnId,
    sessionPeriodeStart,
    sessionPeriodeSlutt,
  } = useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const [gjennomforingId, setGjennomforingId] = useState<string | undefined>(
    sessionGjennomforingId,
  );
  const [tilsagnId, setTilsagnId] = useState<string | undefined>(sessionTilsagnId);

  const hasError = data?.errors && data.errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  const {
    datepickerProps: periodeStartPickerProps,
    inputProps: periodeStartInputProps,
    setSelected: setSelectedFraDato,
  } = useDatepicker({
    defaultSelected: sessionPeriodeStart ? new Date(sessionPeriodeStart) : undefined,
  });
  const {
    datepickerProps: periodeSluttPickerProps,
    inputProps: periodeSluttInputProps,
    setSelected: setSelectedTilDato,
  } = useDatepicker({
    defaultSelected: sessionPeriodeSlutt ? new Date(sessionPeriodeSlutt) : undefined,
  });

  const valgtGjennomforing = gjennomforinger.find((g) => g.id === sessionGjennomforingId);
  const relevanteTilsagn = useMemo(() => {
    if (gjennomforingId) {
      return tilsagn.filter((t) => t.gjennomforing.id === gjennomforingId);
    }
    return [];
  }, [gjennomforingId, tilsagn]);

  return (
    <>
      <Form method="post">
        <VStack gap="6">
          <Heading level="3" size="large">
            Innsendingsinformasjon
          </Heading>
          <GuidePanel className="mb-2">
            <BodyLong spacing>
              I dette skjemaet kan du sende inn krav som gjelder tilsagn for investeringer. Andre
              krav om utbetaling skal sendes inn via utbetalingene i{" "}
              <Link as={ReactRouterLink} to={pathByOrgnr(orgnr).utbetalinger}>
                Utbetalingsoversikten.
              </Link>
            </BodyLong>
          </GuidePanel>
          <VStack gap="6" className="max-w-2xl">
            <TextField readOnly label="Arrangør" size="small" value={`${arrangor} - ${orgnr}`} />
            <input type="hidden" name="orgnr" value={orgnr} />
            <input type="hidden" name="gjennomforingId" value={gjennomforingId} />
            <UNSAFE_Combobox
              size="small"
              label="Velg gjennomføring"
              description="Hvilken gjennomføring gjelder kravet for?"
              error={errorAt("/gjennomforingId", data?.errors)}
              options={gjennomforinger.map((g) => ({
                label: `${g.navn} - ${formaterDato(g.startDato)} - ${g.sluttDato ? formaterDato(g.sluttDato) : ""}`,
                value: g.id,
              }))}
              selectedOptions={
                valgtGjennomforing
                  ? [
                      {
                        label: `${valgtGjennomforing.navn} - ${formaterDato(valgtGjennomforing.startDato)} - ${formaterDato(valgtGjennomforing.sluttDato, "")}`,
                        value: valgtGjennomforing.id,
                      },
                    ]
                  : undefined
              }
              onToggleSelected={(option, isSelected) => {
                if (isSelected) {
                  setGjennomforingId(option);
                } else {
                  setGjennomforingId(undefined);
                  setTilsagnId(undefined);
                }
              }}
            />
            {gjennomforingId && (
              <>
                {relevanteTilsagn.length < 1 ? (
                  <Alert variant="warning">
                    Fant ingen aktive tilsagn for gjennomføringen. Vennligst ta kontakt med Nav.
                  </Alert>
                ) : (
                  <RadioGroup
                    size="small"
                    legend="Velg tilsagn"
                    description="Hvilket tilsagn skal benyttes?"
                    name="tilsagnId"
                    defaultValue={tilsagn.find((t) => t.id === sessionTilsagnId)?.id}
                    error={errorAt("/tilsagnId", data?.errors)}
                    onChange={(val: string) => {
                      setTilsagnId(val);
                      setSelectedFraDato(
                        new Date(tilsagn.find((t) => t.id === val)?.periode.start ?? ""),
                      );
                      setSelectedTilDato(
                        subtractDays(
                          new Date(tilsagn.find((t) => t.id === val)?.periode.slutt ?? ""),
                          1,
                        ),
                      );
                    }}
                  >
                    {relevanteTilsagn.map((tilsagn) => (
                      <Radio key={tilsagn.id} size="small" value={tilsagn.id}>
                        <TilsagnDetaljer key={tilsagn.id} tilsagn={tilsagn} minimal />
                      </Radio>
                    ))}
                  </RadioGroup>
                )}
                {tilsagnId && (
                  <VStack gap="1">
                    <Label size="small">Periode</Label>
                    <BodyShort textColor="subtle" size="small">
                      Hvilken periode gjelder kravet for?
                    </BodyShort>
                    <HStack gap="4">
                      <DatePicker
                        {...periodeStartPickerProps}
                        dropdownCaption
                        id="periodeStartDatepicker"
                      >
                        <DatePicker.Input
                          label="Fra dato"
                          size="small"
                          error={errorAt("/periodeStart", data?.errors)}
                          name="periodeStart"
                          id="periodeStart"
                          {...periodeStartInputProps}
                        />
                      </DatePicker>
                      <DatePicker
                        {...periodeSluttPickerProps}
                        dropdownCaption
                        id="periodeSluttDatepicker"
                      >
                        <DatePicker.Input
                          label="Til dato"
                          size="small"
                          error={errorAt("/periodeSlutt", data?.errors)}
                          name="periodeSlutt"
                          id="periodeSlutt"
                          {...periodeSluttInputProps}
                        />
                      </DatePicker>
                    </HStack>
                  </VStack>
                )}
              </>
            )}
          </VStack>
          {data?.errors && data.errors.length > 0 && (
            <ErrorSummary ref={errorSummaryRef}>
              {data.errors.map((error: FieldError) => {
                return (
                  <ErrorSummary.Item
                    href={`#${jsonPointerToFieldPath(error.pointer)}`}
                    key={jsonPointerToFieldPath(error.pointer)}
                  >
                    {error.detail}
                  </ErrorSummary.Item>
                );
              })}
            </ErrorSummary>
          )}
          <HStack gap="4" className="mt-4">
            <Button type="submit" variant="tertiary" name="intent" value="cancel">
              Avbryt
            </Button>
            <Button type="submit" name="intent" value="submit">
              Neste
            </Button>
          </HStack>
        </VStack>
      </Form>
    </>
  );
}
