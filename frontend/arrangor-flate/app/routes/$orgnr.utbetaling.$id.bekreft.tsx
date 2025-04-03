import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  Button,
  Checkbox,
  ErrorSummary,
  Heading,
  HelpText,
  HStack,
  Link,
  TextField,
  VStack,
} from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrangorflateTilsagn,
  ArrFlateUtbetaling,
  FieldError,
} from "api-client";
import { useEffect, useRef } from "react";
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
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeader } from "~/components/PageHeader";
import { UtbetalingDetaljer } from "~/components/utbetaling/UtbetalingDetaljer";
import { getOrError, getOrThrowError } from "~/form/form-helpers";
import { internalNavigation } from "~/internal-navigation";
import { isValidationError, problemDetailResponse, useOrgnrFromUrl } from "~/utils";
import { getCurrentTab } from "~/utils/currentTab";
import { Separator } from "../components/Separator";

type BekreftUtbetalingData = {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
};

interface ActionData {
  errors?: FieldError[];
}

export const meta: MetaFunction = () => {
  return [
    { title: "Bekreft utbetaling" },
    { name: "description", content: "Arrangørflate for bekreftelse av krav om utbetaling" },
  ];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<BekreftUtbetalingData> => {
  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const [{ data: utbetaling, error: utbetalingError }, { data: tilsagn, error: tilsagnError }] =
    await Promise.all([
      ArrangorflateService.getArrFlateUtbetaling({
        path: { id },
        headers: await apiHeaders(request),
      }),
      ArrangorflateService.getArrangorflateTilsagnTilUtbetaling({
        path: { id },
        headers: await apiHeaders(request),
      }),
    ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }
  if (tilsagnError || !tilsagn) {
    throw problemDetailResponse(tilsagnError);
  }

  return { utbetaling, tilsagn };
};

export const action: ActionFunction = async ({ request }) => {
  const formData = await request.formData();

  const currentTab = getCurrentTab(request);
  const utbetalingId = getOrThrowError(formData, "utbetalingId").toString();
  const utbetalingDigest = getOrThrowError(formData, "utbetalingDigest").toString();
  const orgnr = getOrThrowError(formData, "orgnr").toString();

  const { error: bekreftelseError } = getOrError(
    formData,
    "bekreftelse",
    "Du må bekrefte at opplysningene er korrekte",
  );
  const { error: kontonummerError, data: kontonummer } = getOrError(
    formData,
    "kontonummer",
    "Kontonummer eksisterer ikke",
  );
  const kid = formData.get("kid")?.toString();

  if (kontonummerError || bekreftelseError) {
    return {
      errors: [kontonummerError, bekreftelseError].filter((error) => error !== undefined),
    };
  }

  const validationErrors = validateBetalingsinformasjon(kontonummer.toString(), kid);
  if (validationErrors) {
    return validationErrors;
  }

  const { error } = await ArrangorflateService.godkjennUtbetaling({
    path: { id: utbetalingId },
    body: {
      digest: utbetalingDigest,
      betalingsinformasjon: {
        kontonummer: kontonummer.toString(),
        kid: kid || null,
      },
    },
    headers: await apiHeaders(request),
  });
  if (error) {
    if (isValidationError(error)) {
      return { errors: error.errors };
    } else {
      throw problemDetailResponse(error);
    }
  }
  return redirect(
    `${internalNavigation(orgnr).innsendtUtbetaling(utbetalingId)}?forside-tab=${currentTab}`,
  );
};

function validateBetalingsinformasjon(kontonummer: string, kid?: string) {
  const errors = [];
  const KONTONUMMER_REGEX = /^\d{11}$/;
  const KID_REGEX = /^\d{2,25}$/;

  if (!KONTONUMMER_REGEX.test(kontonummer)) {
    errors.push({ pointer: "/kontonummer", detail: "Kontonummer må være 11 siffer" });
  }

  if (kid && !KID_REGEX.test(kid)) {
    errors.push({
      pointer: "/kid",
      detail: "KID-nummer kan kun inneholde tall og være maks 25 siffer",
    });
  }

  return errors.length > 0 ? { errors } : null;
}

export default function BekreftUtbetaling() {
  const { utbetaling, tilsagn } = useLoaderData<BekreftUtbetalingData>();
  const data = useActionData<ActionData>();
  const orgnr = useOrgnrFromUrl();
  const fetcher = useFetcher();
  const revalidator = useRevalidator();
  const errorSummaryRef = useRef<HTMLDivElement>(null);

  const handleHentKontonummer = async () => {
    fetcher.load(`/api/${utbetaling.id}/sync-kontonummer`);
  };

  useEffect(() => {
    if (
      fetcher.state === "idle" &&
      fetcher.data &&
      fetcher.data !== utbetaling.betalingsinformasjon?.kontonummer
    ) {
      revalidator.revalidate();
    }
  }, [fetcher.state, fetcher.data, revalidator, utbetaling.betalingsinformasjon?.kontonummer]);

  if (data?.errors) {
    errorSummaryRef.current?.focus();
  }

  return (
    <>
      <PageHeader
        title="Oppsummering av utbetaling"
        tilbakeLenke={{
          navn: "Tilbake til beregning",
          url: internalNavigation(orgnr).beregning(utbetaling.id),
        }}
      />
      <VStack className="max-w-[50%]" gap="5">
        <UtbetalingDetaljer utbetaling={utbetaling} tilsagn={tilsagn} />
        <Separator />
        <Heading size="medium">Betalingsinformasjon</Heading>
        <Form method="post">
          <HStack>
            <div>
              <HStack align="end" gap="2">
                <TextField
                  label="Kontonummer"
                  size="small"
                  description="Kontonummeret hentes automatisk fra Altinn"
                  error={data?.errors?.find((error) => error.pointer === "/kontonummer")?.detail}
                  name="kontonummer"
                  defaultValue={utbetaling.betalingsinformasjon?.kontonummer}
                  maxLength={11}
                  minLength={11}
                  id="kontonummer"
                  readOnly
                />
                <HStack align="start" gap="2">
                  <Button
                    type="button"
                    variant="secondary"
                    size="small"
                    onClick={handleHentKontonummer}
                  >
                    Synkroniser kontonummer
                  </Button>
                  <HelpText>
                    Dersom du har oppdatert kontoregisteret via Altinn kan du trykke på knappen
                    "Synkroniser kontonummer" for å hente kontonummeret på nytt fra Altinn.
                  </HelpText>
                </HStack>
              </HStack>
              <small className="mt-2 block">
                Er kontonummeret feil kan du lese her om <EndreKontonummerLink />.
              </small>
              {!utbetaling.betalingsinformasjon?.kontonummer ? (
                <Alert variant="warning" className="my-5">
                  <VStack align="start" gap="2">
                    <Heading spacing size="xsmall" level="3">
                      Fant ikke kontonummer for utbetalingen
                    </Heading>
                    <p>
                      Vi fant ikke noe kontonummer for din organisasjon. Her kan du lese om{" "}
                      <EndreKontonummerLink />.
                    </p>
                    <p className="text-balance">
                      Når du har registrert kontonummer kan du prøve på nytt ved å trykke på knappen{" "}
                      <b>Hent kontonummer</b>.
                    </p>
                  </VStack>
                  <VStack align="end">
                    <Button
                      variant="primary"
                      type="button"
                      size="small"
                      onClick={(e) => {
                        e.preventDefault(); // Prevent form submission
                        handleHentKontonummer();
                      }}
                      disabled={fetcher.state === "loading"}
                    >
                      {fetcher.state === "submitting"
                        ? "Henter kontonummer..."
                        : "Hent kontonummer"}
                    </Button>
                  </VStack>
                </Alert>
              ) : null}
            </div>
          </HStack>
          <HStack>
            <TextField
              className="mt-5"
              label="KID-nummer for utbetaling (valgfritt)"
              size="small"
              name="kid"
              error={data?.errors?.find((error) => error.pointer === "/kid")?.detail}
              defaultValue={utbetaling.betalingsinformasjon?.kid ?? ""}
              maxLength={25}
              id="kid"
            />
          </HStack>
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox
              name="bekreftelse"
              value="bekreftet"
              error={!!data?.errors?.find((error) => error.pointer === "/bekreftelse")?.detail}
              id="bekreftelse"
            >
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
            <input type="hidden" name="utbetalingId" value={utbetaling.id} />
            <input type="hidden" name="utbetalingDigest" value={utbetaling.beregning.digest} />
            <input type="hidden" name="orgnr" value={orgnr} />
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
        </Form>
      </VStack>
    </>
  );
}

function EndreKontonummerLink() {
  return (
    <Link
      rel="noopener noreferrer"
      href="https://www.nav.no/arbeidsgiver/endre-kontonummer#hvordan"
      target="_blank"
    >
      endring av kontonummer for refusjoner fra Nav
    </Link>
  );
}
