import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  Box,
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
  Link as ReactRouterLink,
  redirect,
  useActionData,
  useFetcher,
  useLoaderData,
  useRevalidator,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { KontonummerInput } from "~/components/KontonummerInput";
import { PageHeader } from "~/components/PageHeader";
import { Separator } from "~/components/Separator";
import { getOrError, getOrThrowError } from "~/form/form-helpers";
import { internalNavigation } from "~/internal-navigation";
import {
  formaterDato,
  formaterPeriode,
  isValidationError,
  problemDetailResponse,
  useOrgnrFromUrl,
} from "~/utils";
import { Definisjonsliste } from "../components/Definisjonsliste";
import { TilsagnDetaljer } from "../components/tilsagn/TilsagnDetaljer";
import { tekster } from "../tekster";
import { getBeregningDetaljer } from "../utils/beregning";

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

  const validationErrors = validateKid(kid);
  if (validationErrors) {
    return validationErrors;
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
  return redirect(internalNavigation(orgnr).kvittering(id));
};

export function validateKid(kid: string | null) {
  const errors = [];
  const KID_REGEX = /^\d{2,25}$/;

  if (typeof kid === "string" && !KID_REGEX.test(kid)) {
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
    <VStack className="max-w-[50%]" gap="5">
      <PageHeader
        title="Oppsummering av innsending"
        tilbakeLenke={{
          navn: tekster.bokmal.utbetaling.tilbakeTilBeregning,
          url: internalNavigation(orgnr).beregning(utbetaling.id),
        }}
      />
      <Definisjonsliste
        definitions={[
          { key: "Frist for innsending", value: formaterDato(utbetaling.fristForGodkjenning) },
        ]}
      />
      <Definisjonsliste
        definitions={[
          { key: "Tiltaksnavn", value: utbetaling.gjennomforing.navn },
          { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
        ]}
      />
      <Separator />
      <Heading size="small" level="2">
        Tilsagnsdetaljer
      </Heading>
      {tilsagn.length === 0 && (
        <Alert variant="info">Det finnes ingen godkjente tilsagn for utbetalingsperioden</Alert>
      )}
      {tilsagn.map((t) => (
        <Box
          padding="2"
          key={t.id}
          borderWidth="1"
          borderColor="border-subtle"
          borderRadius="medium"
        >
          <TilsagnDetaljer tilsagn={t} />
        </Box>
      ))}
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
      <Separator />
      <Heading size="small" level="2">
        Betalingsinformasjon
      </Heading>
      <Form method="post">
        <KontonummerInput
          kontonummer={utbetaling.betalingsinformasjon.kontonummer ?? undefined}
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
            {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
          </Checkbox>
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
          <HStack gap="4">
            <Button
              as={ReactRouterLink}
              type="button"
              variant="secondary"
              to={internalNavigation(orgnr).beregning(utbetaling.id)}
            >
              Tilbake
            </Button>
            <Button type="submit">Bekreft og send inn</Button>
          </HStack>
        </VStack>
      </Form>
    </VStack>
  );
}
