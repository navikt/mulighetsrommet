import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Box,
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
  ArrangorflateTilsagnDto,
  ArrangorflateUtbetalingDto,
  FieldError,
} from "api-client";
import { useEffect, useRef } from "react";
import {
  ActionFunction,
  Form,
  Link as ReactRouterLink,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useFetcher,
  useLoaderData,
  useRevalidator,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { KontonummerInput } from "~/components/utbetaling/KontonummerInput";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { tekster } from "~/tekster";
import { UtbetalingManglendeTilsagnAlert } from "~/components/utbetaling/UtbetalingManglendeTilsagnAlert";
import { pathTo, useOrgnrFromUrl } from "~/utils/navigation";
import { errorAt, isValidationError, problemDetailResponse } from "~/utils/validering";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

type BekreftUtbetalingData = {
  utbetaling: ArrangorflateUtbetalingDto;
  tilsagn: ArrangorflateTilsagnDto[];
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
      ArrangorflateService.getArrangorflateUtbetaling({
        path: { id },
        headers: await apiHeaders(request),
      }),
      ArrangorflateService.getArrangorflateTilsagnTilUtbetaling({
        path: { id },
        headers: await apiHeaders(request),
      }),
    ]);

  if (utbetalingError) {
    throw problemDetailResponse(utbetalingError);
  }
  if (tilsagnError) {
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

  const utbetalingDigest = formData.get("utbetalingDigest")?.toString();
  if (!utbetalingDigest) {
    throw new Error(`Mangler ${utbetalingDigest}`);
  }
  const orgnr = formData.get("orgnr")?.toString();
  if (!orgnr) {
    throw new Error(`Mangler ${orgnr}`);
  }

  const errors: FieldError[] = [];

  if (!formData.get("bekreftelse")) {
    errors.push({
      pointer: "/bekreftelse",
      detail: "Du må bekrefte at opplysningene er korrekte",
    });
  }
  if (!formData.get("kontonummer")) {
    errors.push({
      pointer: "/kontonummer",
      detail: "Kontonummer eksisterer ikke",
    });
  }
  const kid = formData.get("kid")?.toString() || null;

  if (errors.length > 0) {
    return { errors };
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
  return redirect(pathTo.kvittering(orgnr, id));
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
      fetcher.data !== utbetaling.betalingsinformasjon
    ) {
      revalidator.revalidate();
    }
  }, [fetcher.state, fetcher.data, revalidator, utbetaling.betalingsinformasjon]);

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
      <Definisjonsliste
        title="Innsendingsinformasjon"
        definitions={[
          {
            key: "Arrangør",
            value: `${utbetaling.arrangor.navn} - ${utbetaling.arrangor.organisasjonsnummer}`,
          },
          {
            key: "Tiltaksnavn",
            value: `${utbetaling.gjennomforing.navn} (${utbetaling.gjennomforing.lopenummer})`,
          },
          { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
        ]}
      />
      <Separator />
      <Definisjonsliste
        title={"Utbetaling"}
        definitions={[
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
        ]}
      />
      <SatsPerioderOgBelop
        pris={utbetaling.beregning.pris}
        satsDetaljer={utbetaling.beregning.satsDetaljer}
      />
      <Separator />
      <Form method="post">
        <Box marginBlock="0 4">
          {harTilsagn ? (
            <>
              <Heading size="medium" level="3" spacing>
                Betalingsinformasjon
              </Heading>
              <VStack gap="4">
                <KontonummerInput
                  kontonummer={utbetaling.betalingsinformasjon?.kontonummer ?? undefined}
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
        </Box>
        <HStack gap="4">
          <Button
            as={ReactRouterLink}
            type="button"
            variant="tertiary"
            to={pathTo.beregning(orgnr, utbetaling.id)}
          >
            Tilbake
          </Button>
          {harTilsagn && <Button type="submit">Bekreft og send inn</Button>}
        </HStack>
      </Form>
    </>
  );
}
