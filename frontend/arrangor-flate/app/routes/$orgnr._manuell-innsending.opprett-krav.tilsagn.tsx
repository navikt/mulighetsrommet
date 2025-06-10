import {
  Alert,
  BodyShort,
  Button,
  DatePicker,
  ErrorSummary,
  Heading,
  HStack,
  Label,
  Radio,
  RadioGroup,
  UNSAFE_Combobox,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import {
  ArrangorflateGjennomforing,
  ArrangorflateService,
  ArrangorflateTilsagn,
  FieldError,
  TilsagnType,
} from "api-client";
import { useMemo, useRef, useState } from "react";
import {
  LoaderFunction,
  MetaFunction,
  useActionData,
  useLoaderData,
  Link as ReactRouterLink,
  ActionFunctionArgs,
  redirect,
  Form,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { formaterDato, problemDetailResponse } from "~/utils";
import { internalNavigation } from "../internal-navigation";
import { errorAt } from "~/utils/validering";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { commitSession, getSession } from "~/sessions.server";

type LoaderData = {
  gjennomforinger: ArrangorflateGjennomforing[];
  tilsagn: ArrangorflateTilsagn[];
  orgnr: string;
  sessionGjennomforingId?: string;
  sessionTilsagnId?: string;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Manuell innsending" },
    { name: "description", content: "Manuell innsending av krav om utbetaling" },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  const session = await getSession(request.headers.get("Cookie"));
  const sessionGjennomforingId = session.get("gjennomforingId");
  const sessionTilsagnId = session.get("tilsagnId");
  const sessionPeriodeStart = session.get("periodeStart");
  const sessionPeriodeSlutt = session.get("periodeSlutt");

  const [
    { data: gjennomforinger, error: gjennomforingerError },
    { data: tilsagn, error: tilsagnError },
  ] = await Promise.all([
    ArrangorflateService.getArrangorflateGjennomforinger({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getAllArrangorflateTilsagn({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
  ]);

  if (gjennomforingerError) {
    throw problemDetailResponse(gjennomforingerError);
  }
  if (tilsagnError || !tilsagn) {
    throw problemDetailResponse(tilsagnError);
  }
  return {
    orgnr,
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

  const orgnr = formData.get("orgnr")?.toString();
  const periodeStart = formData.get("periodeStart")?.toString();
  const periodeSlutt = formData.get("periodeSlutt")?.toString();
  const gjennomforingId = formData.get("gjennomforingId")?.toString();
  const tilsagnId = formData.get("tilsagnId")?.toString();

  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
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
    session.set("orgnr", orgnr);
    session.set("gjennomforingId", gjennomforingId);
    session.set("tilsagnId", tilsagnId);
    session.set("periodeStart", periodeStart);
    session.set("periodeSlutt", periodeSlutt);
    return redirect(internalNavigation(orgnr).opprettKravVedlegg, {
      headers: {
        "Set-Cookie": await commitSession(session),
      },
    });
  }
}

interface ActionData {
  errors: FieldError[];
}

export default function OpprettKravTilsagn() {
  const {
    orgnr,
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
      return tilsagn.filter(
        (t) => t.gjennomforing.id === gjennomforingId && t.type === TilsagnType.INVESTERING,
      );
    }
    return [];
  }, [gjennomforingId, tilsagn]);

  return (
    <>
      <Heading size="large" spacing level="2">
        Tilsagn
      </Heading>
      <Form method="post">
        <VStack gap="6" className="max-w-xl">
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
                      label: `${valgtGjennomforing.navn} - ${formaterDato(valgtGjennomforing.startDato)} - ${valgtGjennomforing.sluttDato ? formaterDato(valgtGjennomforing.sluttDato) : ""}`,
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
                      new Date(tilsagn.find((t) => t.id === val)?.periode.slutt ?? ""),
                    );
                  }}
                >
                  {relevanteTilsagn.map((tilsagn) => (
                    <Radio key={tilsagn.id} size="small" value={tilsagn.id}>
                      <TilsagnDetaljer key={tilsagn.id} tilsagn={tilsagn} />
                    </Radio>
                  ))}
                </RadioGroup>
              )}
              {tilsagnId && (
                <VStack gap="2">
                  <Label size="small">Periode</Label>
                  <BodyShort textColor="subtle" spacing size="small">
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
          <HStack className="mt-4">
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={internalNavigation(orgnr).utbetalinger}
            >
              Avbryt
            </Button>
            <Button type="submit">Neste</Button>
          </HStack>
        </VStack>
      </Form>
    </>
  );
}
