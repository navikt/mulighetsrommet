import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Button,
  Checkbox,
  ErrorSummary,
  Heading,
  HStack,
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
import { KontonummerInput } from "~/components/KontonummerInput";

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

  return {
    utbetaling,
    tilsagn,
  };
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

export function validateBetalingsinformasjon(kontonummer: string, kid?: string) {
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
          <KontonummerInput
            kontonummer={utbetaling.betalingsinformasjon.kontonummer}
            error={data?.errors?.find((error) => error.pointer === "/kontonummer")?.detail}
            onClick={() => handleHentKontonummer()}
          />
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
