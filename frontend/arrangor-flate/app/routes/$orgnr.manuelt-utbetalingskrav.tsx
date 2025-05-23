import { FileUpload, FileUploadHandler, parseFormData } from "@mjackson/form-data-parser";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  Button,
  Checkbox,
  DatePicker,
  ErrorSummary,
  Heading,
  HStack,
  Select,
  Textarea,
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
  TilsagnType,
  Tilskuddstype,
} from "api-client";
import { useMemo, useRef, useState } from "react";
import {
  ActionFunction,
  Form,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useFetcher,
  useLoaderData,
  useRevalidator,
  Link as ReactRouterLink,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { KontonummerInput } from "~/components/KontonummerInput";
import { PageHeader } from "~/components/PageHeader";
import { Separator } from "~/components/Separator";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { formaterDato, isValidationError, problemDetailResponse, useOrgnrFromUrl } from "~/utils";
import { FileUploader } from "../components/fileUploader/FileUploader";
import { internalNavigation } from "../internal-navigation";
import { tekster } from "../tekster";
import css from "../root.module.css";

const MIN_BESKRIVELSE_LENGTH = 10;
const MAX_BESKRIVELSE_LENGTH = 500;

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

const uploadHandler: FileUploadHandler = async (fileUpload: FileUpload) => {
  if (fileUpload.fieldName === "vedlegg" && fileUpload.name.endsWith(".pdf")) {
    // process the upload and return a File
    const bytes = await fileUpload.bytes();
    return new File([bytes], fileUpload.name, { type: fileUpload.type });
  }
};

export const action: ActionFunction = async ({ request }) => {
  const formData = await parseFormData(
    request,
    {
      maxFileSize: 10000000, // 10MB
    },
    uploadHandler,
  );

  const orgnr = formData.get("orgnr")?.toString();
  const bekreftelse = formData.get("bekreftelse")?.toString();
  const vedlegg = formData.getAll("vedlegg") as File[];
  const beskrivelse = formData.get("beskrivelse")?.toString();
  const kontonummer = formData.get("kontonummer")?.toString();
  const periodeStart = formData.get("periodeStart")?.toString();
  const periodeSlutt = formData.get("periodeSlutt")?.toString();
  const gjennomforingId = formData.get("gjennomforingId")?.toString();
  const belop = Number(formData.get("belop")?.toString());
  const tilskuddstype = formData.get("tilskuddstype")?.toString();
  const kid = formData.get("kid")?.toString();
  const errors: FieldError[] = [];

  if (
    !tilskuddstype ||
    ![Tilskuddstype.TILTAK_DRIFTSTILSKUDD, Tilskuddstype.TILTAK_INVESTERINGER].includes(
      tilskuddstype as Tilskuddstype,
    )
  ) {
    if (!periodeStart) {
      errors.push({
        pointer: "/periodeStart",
        detail: "Du må fylle ut fra dato",
      });
    }
    if (!periodeSlutt) {
      errors.push({
        pointer: "/periodeSlutt",
        detail: "Du må fylle ut til dato",
      });
    }
    errors.push({
      pointer: "/tilskuddstype",
      detail: "Du må fylle ut type",
    });
  }

  if (!gjennomforingId) {
    errors.push({
      pointer: "/gjennomforingId",
      detail: "Du må velge gjennomføring",
    });
  }

  if (!belop) {
    errors.push({
      pointer: "/belop",
      detail: "Du må fylle ut beløp",
    });
  }

  if (vedlegg.length === 0) {
    errors.push({
      pointer: "/vedlegg",
      detail: "Du må laste opp minst ett vedlegg",
    });
  }

  if (!beskrivelse) {
    errors.push({
      pointer: "/beskrivelse",
      detail: "Du må fylle ut beskrivelsen",
    });
  }

  if (beskrivelse && beskrivelse.length < MIN_BESKRIVELSE_LENGTH) {
    errors.push({
      pointer: "/beskrivelse",
      detail: `Beskrivelse må være minst ${MIN_BESKRIVELSE_LENGTH} tegn`,
    });
  }

  if (beskrivelse && beskrivelse.length > MAX_BESKRIVELSE_LENGTH) {
    errors.push({
      pointer: "/beskrivelse",
      detail: `Beskrivelse kan være maks ${MAX_BESKRIVELSE_LENGTH} tegn`,
    });
  }

  if (!tilskuddstype || !Object.values(Tilskuddstype).includes(tilskuddstype as Tilskuddstype)) {
    errors.push({
      pointer: "/tilskuddstype",
      detail: "Du må velge type",
    });
  }

  if (!kontonummer) {
    errors.push({
      pointer: "/kontonummer",
      detail: "Fant ikke kontonummer",
    });
  }

  if (!bekreftelse) {
    errors.push({
      pointer: "/bekreftelse",
      detail: "Du må bekrefte at opplysningene er korrekte",
    });
  }

  if (errors.length > 0) {
    return { errors };
  }

  const { error, data: utbetalingId } =
    await ArrangorflateService.opprettArrangorflateManuellUtbetaling({
      path: { orgnr: orgnr! },
      body: {
        tilskuddstype: tilskuddstype as Tilskuddstype,
        belop: belop,
        gjennomforingId: gjennomforingId!,
        beskrivelse: beskrivelse!,
        periodeStart: formaterDatoSomYYYYMMDD(periodeStart!),
        periodeSlutt: formaterDatoSomYYYYMMDD(periodeSlutt!),
        kontonummer: kontonummer!,
        kidNummer: kid || null,
        vedlegg: vedlegg,
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
    return redirect(`${internalNavigation(orgnr!).kvittering(utbetalingId)}`);
  }
};

function datePickerProps(onDateChange: (val?: Date | undefined) => void) {
  return {
    fromDate: new Date(2024, 2, 31),
    toDate: new Date(2030, 12, 31),
    onDateChange: onDateChange,
  };
}

export default function ManuellUtbetalingForm() {
  const { kontonummer, gjennomforinger, tilsagn } = useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const orgnr = useOrgnrFromUrl();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const revalidator = useRevalidator();
  const fetcher = useFetcher();
  const [gjennomforingId, setGjennomforingId] = useState<string | undefined>();
  const [periodeStart, setPeriodeStart] = useState<string | undefined>();
  const [periodeSlutt, setPeriodeSlutt] = useState<string | undefined>();
  const [tilskuddstype, setTilskuddstype] = useState<Tilskuddstype | undefined>();
  const { datepickerProps: periodeStartPickerProps, inputProps: periodeStartInputProps } =
    useDatepicker({
      ...datePickerProps((val?: Date | undefined) => setPeriodeStart(val?.toISOString())),
    });

  const { datepickerProps: periodeSluttPickerProps, inputProps: periodeSluttInputProps } =
    useDatepicker({
      ...datePickerProps((val?: Date | undefined) => setPeriodeSlutt(val?.toISOString())),
    });

  function errorAt(pointer: string): string | undefined {
    return data?.errors?.find((error) => error.pointer === pointer)?.detail;
  }

  const relevanteTilsagn = useMemo(() => {
    if (!periodeStart || !periodeSlutt) {
      return [];
    }

    const start = new Date(periodeStart);
    const slutt = new Date(periodeSlutt);

    if (gjennomforingId && !isNaN(start.getTime()) && !isNaN(slutt.getTime())) {
      return tilsagn
        .filter((t) => t.gjennomforing.id === gjennomforingId)
        .filter((t) => {
          if (tilskuddstype === Tilskuddstype.TILTAK_DRIFTSTILSKUDD) {
            return [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN].includes(t.type);
          } else if (tilskuddstype === Tilskuddstype.TILTAK_INVESTERINGER) {
            return [TilsagnType.INVESTERING].includes(t.type);
          }
          return false;
        })
        .filter((t) => start < new Date(t.periode.slutt) && slutt > new Date(t.periode.start));
    }

    return [];
  }, [gjennomforingId, periodeStart, periodeSlutt, tilsagn, tilskuddstype]);
  return (
    <VStack gap="4" className={css.side}>
      <PageHeader
        title={tekster.bokmal.utbetaling.opprettUtbetalingKnapp}
        tilbakeLenke={{
          navn: tekster.bokmal.tilbakeTilOversikt,
          url: internalNavigation(orgnr).utbetalinger,
        }}
      />
      <Form method="post" encType="multipart/form-data" className="max-w-[50%]">
        <VStack gap="6">
          <VStack gap="4" className="max-w-[50%]">
            <Heading as="legend" level="3" size="medium">
              Innsending
            </Heading>
            <input type="hidden" name="orgnr" value={orgnr} />
            <HStack gap="4">
              <DatePicker {...periodeStartPickerProps} dropdownCaption>
                <DatePicker.Input
                  label="Fra dato"
                  size="small"
                  error={errorAt("/periodeStart")}
                  name="periodeStart"
                  id="periodeStart"
                  {...periodeStartInputProps}
                />
              </DatePicker>
              <DatePicker {...periodeSluttPickerProps} dropdownCaption>
                <DatePicker.Input
                  label="Til dato"
                  size="small"
                  error={errorAt("/periodeSlutt")}
                  name="periodeSlutt"
                  id="periodeSlutt"
                  {...periodeSluttInputProps}
                />
              </DatePicker>
            </HStack>
            <Select
              error={errorAt("/tilskuddstype")}
              label="Velg type utbetaling"
              name="tilskuddstype"
              size="small"
              id="tilskuddstype"
              onChange={(e) => {
                setTilskuddstype(e.target.value as Tilskuddstype);
              }}
            >
              <option>- Velg type -</option>
              <option value={Tilskuddstype.TILTAK_INVESTERINGER}>Investering</option>
              <option value={Tilskuddstype.TILTAK_DRIFTSTILSKUDD}>Drift</option>
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
          </VStack>
          <Separator />
          <VStack gap="4">
            <Heading level="3" size="medium">
              Tilsagn
            </Heading>
            {relevanteTilsagn.length < 1 && (
              <Alert variant="info">
                Fant ingen relevante tilsagn for gjennomføring i perioden. Det er fortsatt mulig å
                sende inn.
              </Alert>
            )}
            {relevanteTilsagn.map((tilsagn) => (
              <TilsagnDetaljer
                tilsagn={tilsagn}
                ekstraDefinisjoner={[
                  { key: "Tilsagnsnummer", value: tilsagn.bestillingsnummer },
                  {
                    key: "Tilsagnstype",
                    value: tekster.bokmal.tilsagn.tilsagntype(tilsagn.type),
                  },
                ]}
              />
            ))}
          </VStack>
          <Separator />
          <VStack gap="4">
            <Heading level="3" size="medium">
              Vedlegg
            </Heading>
            <FileUploader
              error={errorAt("/vedlegg")}
              maxFiles={10}
              maxSizeMB={3}
              maxSizeBytes={3 * 1024 * 1024}
              id="vedlegg"
            />
            <Textarea
              label="Beskrivelse"
              description="Her kan du spesifisere hva utbetalingen gjelder"
              name="beskrivelse"
              error={errorAt("/beskrivelse")}
              id="beskrivelse"
              minLength={10}
              maxLength={500}
            />
          </VStack>
          <Separator />
          <VStack className="max-w-[50%]">
            <Heading level="3" spacing size="medium">
              Utbetaling
            </Heading>
            <TextField
              label="Beløp til utbetaling"
              error={errorAt("/belop")}
              htmlSize={35}
              size="small"
              name="belop"
              id="belop"
            />
          </VStack>
          <Separator />
          <VStack gap="4">
            <Heading level="3" size="medium">
              Betalingsinformasjon
            </Heading>
            <KontonummerInput
              kontonummer={kontonummer}
              error={errorAt("/kontonummer")}
              onClick={() => revalidator.revalidate()}
            />
            <TextField
              label="KID-nummer for utbetaling (valgfritt)"
              size="small"
              name="kid"
              error={errorAt("/kid")}
              htmlSize={35}
              maxLength={25}
              id="kid"
            />
          </VStack>
          <Separator />
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
          <HStack>
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={internalNavigation(orgnr).utbetalinger}
            >
              Avbryt
            </Button>
            <Button disabled={fetcher.state !== "idle"} type="submit">
              {fetcher.state === "submitting" ? "Sender inn..." : "Bekreft og send inn"}
            </Button>
          </HStack>
        </VStack>
      </Form>
    </VStack>
  );
}

function formaterDatoSomYYYYMMDD(dato: string | Date | null, fallback = ""): string {
  if (!dato) return fallback;

  let dateObj: Date;
  if (typeof dato === "string") {
    const [day, month, year] = dato.split(".").map(Number);
    dateObj = new Date(year, month - 1, day);
  } else {
    dateObj = dato;
  }

  if (isNaN(dateObj.getTime())) return fallback;

  const year = dateObj.getFullYear();
  const month = String(dateObj.getMonth() + 1).padStart(2, "0");
  const day = String(dateObj.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
