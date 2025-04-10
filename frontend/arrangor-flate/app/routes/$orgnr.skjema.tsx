import {
  Alert,
  Box,
  Button,
  Checkbox,
  ErrorSummary,
  HStack,
  Select,
  Textarea,
  TextField,
  UNSAFE_Combobox,
  VStack,
} from "@navikt/ds-react";
import { Separator } from "~/components/Separator";
import { internalNavigation } from "../internal-navigation";
import {
  ActionFunction,
  Form,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useLoaderData,
  useRevalidator,
} from "react-router";
import { formaterDato, isValidationError, problemDetailResponse, useOrgnrFromUrl } from "~/utils";
import { PageHeader } from "~/components/PageHeader";
import {
  ArrangorflateGjennomforing,
  ArrangorflateService,
  ArrangorflateTilsagn,
  FieldError,
} from "api-client";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useMemo, useRef, useState } from "react";
import { apiHeaders } from "~/auth/auth.server";
import { KontonummerInput } from "~/components/KontonummerInput";
import { getCurrentTab } from "~/utils/currentTab";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";

type LoaderData = {
  kontonummer?: string;
  gjennomforinger: ArrangorflateGjennomforing[];
  tilsagn: ArrangorflateTilsagn[];
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

  const [
    { data: kontonummer, error: kontonummerError },
    { data: gjennomforinger, error: gjennomforingerError },
    { data: tilsagn, error: tilsagnError },
  ] = await Promise.all([
    ArrangorflateService.getKontonummer({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getArrangorflateGjennomforinger({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getAllArrangorflateTilsagn({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
  ]);

  if (kontonummerError) {
    throw problemDetailResponse(kontonummerError);
  }
  if (gjennomforingerError) {
    throw problemDetailResponse(gjennomforingerError);
  }
  if (tilsagnError || !tilsagn) {
    throw problemDetailResponse(tilsagnError);
  }
  return { kontonummer, gjennomforinger, tilsagn };
};

interface ActionData {
  errors?: FieldError[];
}

export const action: ActionFunction = async ({ request }) => {
  const formData = await request.formData();
  const currentTab = getCurrentTab(request);

  const orgnr = formData.get("orgnr")?.toString();
  const bekreftelse = formData.get("bekreftelse")?.toString();
  const beskrivelse = formData.get("beskrivelse")?.toString();
  const kontonummer = formData.get("kontonummer")?.toString();
  const periodeStart = formData.get("periodeStart")?.toString();
  const periodeSlutt = formData.get("periodeSlutt")?.toString();
  const gjennomforingId = formData.get("gjennomforingId")?.toString();
  const belop = Number(formData.get("belop")?.toString());
  const type = formData.get("type")?.toString();
  const kid = formData.get("kid")?.toString();

  const errors: FieldError[] = [];

  if (!bekreftelse) {
    errors.push({
      pointer: "/bekreftelse",
      detail: "Du må bekrefte at opplysningene er korrekte",
    });
  }
  if (!kontonummer) {
    errors.push({
      pointer: "/kontonummer",
      detail: "Fant ikke kontonummer",
    });
  }
  if (!gjennomforingId) {
    errors.push({
      pointer: "/gjennomforingId",
      detail: "Du må velge gjennomføring",
    });
  }
  if (!beskrivelse) {
    errors.push({
      pointer: "/beskrivelse",
      detail: "Du må fylle ut beskrivelsen",
    });
  }
  if (!periodeStart) {
    errors.push({
      pointer: "/periodeStart",
      detail: "Du må fylle ut periode start",
    });
  }
  if (!periodeSlutt) {
    errors.push({
      pointer: "/periodeSlutt",
      detail: "Du må fylle ut periode slutt",
    });
  }
  if (!belop) {
    errors.push({
      pointer: "/belop",
      detail: "Du må fylle ut beløp",
    });
  }
  if (!type || !["DRIFT", "INVESTERING"].includes(type)) {
    errors.push({
      pointer: "/type",
      detail: "Du må fylle ut type",
    });
  }
  if (errors.length > 0) {
    return { errors };
  }

  const { error, data: utbetalingId } =
    await ArrangorflateService.opprettArrangorflateManuellUtbetaling({
      path: { orgnr: orgnr! },
      body: {
        type: type as "DRIFT" | "INVESTERING",
        belop: belop,
        gjennomforingId: gjennomforingId!,
        beskrivelse: beskrivelse!,
        periodeStart: periodeStart!,
        periodeSlutt: periodeSlutt!,
        kontonummer: kontonummer!,
        kidNummer: kid || null,
      },
      headers: await apiHeaders(request),
    });

  if (error) {
    if (isValidationError(error)) {
      return { errors: error.errors };
    } else {
      throw problemDetailResponse(error);
    }
  } else {
    return redirect(
      `${internalNavigation(orgnr!).innsendtUtbetaling(utbetalingId)}?forside-tab=${currentTab}`,
    );
  }
};

export default function UtbetalingKvittering() {
  const { kontonummer, gjennomforinger, tilsagn } = useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const orgnr = useOrgnrFromUrl();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const revalidator = useRevalidator();

  const [gjennomforingId, setGjennomforingId] = useState<string | undefined>(undefined);
  const [periodeStart, setPeriodeStart] = useState<string>("");
  const [periodeSlutt, setPeriodeSlutt] = useState<string>("");

  function errorAt(pointer: string): string | undefined {
    return data?.errors?.find((error) => error.pointer === pointer)?.detail;
  }

  const relevanteTilsagn = useMemo(() => {
    const start = new Date(periodeStart);
    const slutt = new Date(periodeSlutt);

    if (gjennomforingId && !isNaN(start.getTime()) && !isNaN(slutt.getTime())) {
      return tilsagn
        .filter((t) => t.gjennomforing.id === gjennomforingId)
        .filter((t) => start < new Date(t.periode.slutt) && slutt > new Date(t.periode.start));
    }

    return [];
  }, [gjennomforingId, periodeStart, periodeSlutt, tilsagn]);

  return (
    <>
      <PageHeader
        title="Manuell innsending"
        tilbakeLenke={{
          navn: "Tilbake til utbetalinger",
          url: internalNavigation(orgnr).utbetalinger,
        }}
      />
      <Form method="post">
        <input type="hidden" name="orgnr" value={orgnr} />
        <VStack gap="4" className="max-w-[50%]">
          <Select
            error={errorAt("/type")}
            label="Velg type utbetaling"
            description="TODO: denne gjør ikke noe ennå. Må implementere utbetalingstype"
            name="type"
            size="small"
          >
            <option>- Velg type -</option>
            <option value="INVESTERING">Investering</option>
            <option value="DRIFT">Drift</option>
          </Select>
          <input type="hidden" name="gjennomforingId" value={gjennomforingId} />
          <UNSAFE_Combobox
            size="small"
            label="Velg gjennomføring"
            error={errorAt("/gjennomforingId")}
            options={gjennomforinger.map((g) => ({
              label: `${g.navn} - ${formaterDato(g.startDato)} - ${g.sluttDato ? formaterDato(g.sluttDato) : ""}`,
              value: g.id,
            }))}
            onToggleSelected={(option, isSelected) => {
              if (isSelected) {
                setGjennomforingId(option);
              } else {
                setGjennomforingId(undefined);
              }
            }}
          />
          <TextField
            label="Beløp til utbetaling"
            error={errorAt("/belop")}
            size="small"
            name="belop"
            id="belop"
          />
          <HStack gap="4">
            <TextField
              label="Periodestart"
              error={errorAt("/periodeStart")}
              size="small"
              onChange={(e) => setPeriodeStart(e.target.value)}
              name="periodeStart"
              id="periodeStart"
            />
            <TextField
              label="Periodeslutt"
              size="small"
              onChange={(e) => setPeriodeSlutt(e.target.value)}
              error={errorAt("/periodeSlutt")}
              name="periodeSlutt"
              id="periodeSlutt"
            />
          </HStack>
          <Separator />
          {relevanteTilsagn.length > 0 ? (
            relevanteTilsagn.map((tilsagn) => (
              <Box borderColor="border-subtle" padding="2" borderWidth="2" borderRadius="large">
                <TilsagnDetaljer tilsagn={tilsagn} />
              </Box>
            ))
          ) : (
            <Alert variant="info" className="my-5">
              Fant ingen relevante tilsagn for gjennomføring i perioden
            </Alert>
          )}
          <Separator />
          <Textarea
            label="Beskrivelse"
            description="Her kan du spesifisere hva utbetalingen gjelder"
            size="small"
            name="beskrivelse"
            error={errorAt("/beskrivelse")}
            id="beskrivelse"
          />
          <KontonummerInput
            kontonummer={kontonummer}
            error={errorAt("/kontonummer")}
            onClick={() => revalidator.revalidate()}
          />
          <TextField
            className="mt-5"
            label="KID-nummer for utbetaling (valgfritt)"
            size="small"
            name="kid"
            error={errorAt("/kid")}
            maxLength={25}
            id="kid"
          />
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox
              name="bekreftelse"
              value="bekreftet"
              error={Boolean(errorAt("/bekreftelse"))}
              id="bekreftelse"
            >
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
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
            <Button type="submit">Bekreft og send inn</Button>
          </VStack>
        </VStack>
      </Form>
    </>
  );
}
