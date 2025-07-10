import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Button,
  Checkbox,
  CheckboxGroup,
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
  Link as ReactRouterLink,
  redirect,
  useActionData,
  useFetcher,
  useLoaderData,
  useRevalidator,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { KontonummerInput } from "~/components/utbetaling/KontonummerInput";
import { Separator } from "~/components/common/Separator";
import { getOrError, getOrThrowError } from "~/utils/form-helpers";
import { Definisjonsliste } from "../components/common/Definisjonsliste";
import { tekster } from "../tekster";
import { getBeregningDetaljer } from "../utils/beregning";
import { UtbetalingManglendeTilsagnAlert } from "~/components/utbetaling/UtbetalingManglendeTilsagnAlert";
import { ManglendeMidlerAlert } from "~/components/utbetaling/ManglendeMidlerAlert";
import { formaterPeriode } from "~/utils/date";
import { pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";
import { problemDetailResponse, isValidationError, errorAt } from "~/utils/validering";

type BekreftUtbetalingData = {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
};

interface ActionData {
  errors?: FieldError[];
}

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 3 av 3: Oppsummering - Godkjenn innsending" },
    {
      name: "description",
      content: "Oppsummering av innsendingen og betalingsinformasjon",
    },
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

export const action: ActionFunction = async ({ params, request }) => {
  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const formData = await request.formData();

  const utbetalingDigest = getOrThrowError(formData, "utbetalingDigest").toString();
  const orgnr = getOrThrowError(formData, "orgnr").toString();

  const { error: bekreftelseError } = getOrError(
    formData,
    "bekreftelse",
    "Du må bekrefte at opplysningene er korrekte",
  );
  const { error: kontonummerError } = getOrError(
    formData,
    "kontonummer",
    "Kontonummer eksisterer ikke",
  );
  const kid = formData.get("kid")?.toString() || null;

  if (kontonummerError || bekreftelseError) {
    return {
      errors: [kontonummerError, bekreftelseError].filter((error) => error !== undefined),
    };
  }

  const { error } = await ArrangorflateService.godkjennUtbetaling({
    path: { id },
    body: {
      digest: utbetalingDigest,
      kid,
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
  return redirect(pathByOrgnr(orgnr).kvittering(id));
};

export default function BekreftUtbetaling() {
  const { utbetaling, tilsagn } = useLoaderData<BekreftUtbetalingData>();
  const data = useActionData<ActionData>();
  const orgnr = useOrgnrFromUrl();
  const fetcher = useFetcher();
  const revalidator = useRevalidator();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const hasError = data?.errors && data.errors.length > 0;

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

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  if (data?.errors) {
    errorSummaryRef.current?.focus();
  }

  const harTilsagn = tilsagn.length > 0;

  return (
    <>
      <Heading level="2" spacing size="large">
        Oppsummering
      </Heading>
      <VStack gap="6">
        <Definisjonsliste
          title="Innsendingsinformasjon"
          headingLevel="3"
          definitions={[
            {
              key: "Arrangør",
              value: `${utbetaling.arrangor.navn} - ${utbetaling.arrangor.organisasjonsnummer}`,
            },
            { key: "Tiltaksnavn", value: utbetaling.gjennomforing.navn },
            { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
          ]}
        />
        <Separator />
        <Definisjonsliste
          title={"Utbetaling"}
          headingLevel="3"
          definitions={[
            {
              key: "Utbetalingsperiode",
              value: formaterPeriode(utbetaling.periode),
            },
            ...getBeregningDetaljer(utbetaling.beregning),
          ]}
        />
        <ManglendeMidlerAlert tilsagn={tilsagn} belopTilUtbetaling={utbetaling.beregning.belop} />
        <Separator />
        <Form method="post">
          <VStack gap="6">
            {harTilsagn ? (
              <>
                <VStack gap="4">
                  <Heading size="medium" level="3">
                    Betalingsinformasjon
                  </Heading>
                  <KontonummerInput
                    kontonummer={utbetaling.betalingsinformasjon.kontonummer ?? undefined}
                    error={data?.errors?.find((error) => error.pointer === "/kontonummer")?.detail}
                    onClick={() => handleHentKontonummer()}
                  />
                  <TextField
                    label="KID-nummer for utbetaling (valgfritt)"
                    size="small"
                    name="kid"
                    htmlSize={35}
                    error={data?.errors?.find((error) => error.pointer === "/kid")?.detail}
                    defaultValue={utbetaling.betalingsinformasjon?.kid ?? ""}
                    maxLength={25}
                    id="kid"
                  />
                </VStack>
                <Separator />
                <CheckboxGroup error={errorAt("/bekreftelse", data?.errors)} legend="Bekreftelse">
                  <Checkbox
                    name="bekreftelse"
                    value="bekreftet"
                    id="bekreftelse"
                    error={errorAt("/bekreftelse", data?.errors) !== undefined}
                  >
                    {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
                  </Checkbox>
                </CheckboxGroup>

                <input type="hidden" name="utbetalingDigest" value={utbetaling.beregning.digest} />
                <input type="hidden" name="orgnr" value={orgnr} />
                {hasError && (
                  <ErrorSummary ref={errorSummaryRef}>
                    {data.errors?.map((error: FieldError) => {
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
              </>
            ) : (
              <UtbetalingManglendeTilsagnAlert />
            )}
            <HStack gap="4">
              <Button
                as={ReactRouterLink}
                type="button"
                variant="tertiary"
                to={pathByOrgnr(orgnr).beregning(utbetaling.id)}
              >
                Tilbake
              </Button>
              {harTilsagn && <Button type="submit">Bekreft og send inn</Button>}
            </HStack>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
