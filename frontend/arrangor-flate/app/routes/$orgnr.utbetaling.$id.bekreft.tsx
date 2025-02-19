import {
  ArrangorflateService,
  ArrangorflateTilsagn,
  ArrFlateUtbetaling,
  FieldError,
} from "@mr/api-client-v2";
import { Button, Checkbox, ErrorSummary, Heading, TextField, VStack } from "@navikt/ds-react";
import {
  ActionFunction,
  Form,
  LoaderFunction,
  redirect,
  useActionData,
  useLoaderData,
} from "react-router";
import { Definisjon } from "~/components/Definisjon";
import { PageHeader } from "~/components/PageHeader";
import { UtbetalingDetaljer } from "~/components/utbetaling/UtbetalingDetaljer";
import { getOrError, getOrThrowError } from "~/form/form-helpers";
import { internalNavigation } from "~/internal-navigation";
import { useOrgnrFromUrl } from "~/utils";
import { getCurrentTab } from "~/utils/currentTab";
import { Separator } from "../components/Separator";
import { apiHeaders } from "~/auth/auth.server";
import { isValidationError, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";

type BekreftUtbetalingData = {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
};

interface ActionData {
  errors?: FieldError[];
}

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<BekreftUtbetalingData> => {
  const { id } = params;
  if (!id) throw Error("Mangler id");

  const [{ data: utbetaling }, { data: tilsagn }] = await Promise.all([
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getArrangorflateTilsagnTilUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

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
    "Du må fylle ut kontonummer",
  );
  const kid = formData.get("kid")?.toString();

  if (kontonummerError || bekreftelseError) {
    return {
      errors: [kontonummerError, bekreftelseError].filter((error) => error !== undefined),
    };
  }

  try {
    await ArrangorflateService.godkjennUtbetaling({
      path: { id: utbetalingId },
      body: {
        digest: utbetalingDigest,
        betalingsinformasjon: {
          kontonummer: kontonummer.toString(),
          kid: kid,
        },
      },
      headers: await apiHeaders(request),
    });
  } catch (error) {
    if (isValidationError(error)) {
      return { errors: error.errors };
    }
    throw error;
  }
  return redirect(
    `${internalNavigation(orgnr).kvittering(utbetalingId)}?forside-tab=${currentTab}`,
  );
};

export default function BekreftUtbetaling() {
  const { utbetaling, tilsagn } = useLoaderData<BekreftUtbetalingData>();
  const data = useActionData<ActionData>();
  const orgnr = useOrgnrFromUrl();
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
          <dl>
            <Definisjon label="Kontonummer">
              <TextField
                label="Kontonummer"
                hideLabel
                size="small"
                error={data?.errors?.find((error) => error.pointer === "/kontonummer")?.detail}
                name="kontonummer"
                className="border border-[#0214317D] rounded-md"
                defaultValue={utbetaling.betalingsinformasjon?.kontonummer}
                maxLength={11}
                minLength={11}
              />
            </Definisjon>
            <Definisjon label="Evt KID nr for utbetaling" className="my-4 flex">
              <div className="flex">
                <TextField
                  label="Evt KID nr for utbetaling"
                  hideLabel
                  size="small"
                  name="kid"
                  className="border border-[#0214317D] rounded-md"
                  defaultValue={utbetaling.betalingsinformasjon?.kid}
                  maxLength={25}
                />
              </div>
            </Definisjon>
          </dl>
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox
              name="bekreftelse"
              value="bekreftet"
              error={!!data?.errors?.find((error) => error.pointer === "/bekreftelse")?.detail}
            >
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
            <input type="hidden" name="utbetalingId" value={utbetaling.id} />
            <input type="hidden" name="utbetalingDigest" value={utbetaling.beregning.digest} />
            <input type="hidden" name="orgnr" value={orgnr} />
            {data?.errors && data.errors.length > 0 && (
              <ErrorSummary>
                {data.errors.map((error: FieldError) => {
                  return (
                    <ErrorSummary.Item key={jsonPointerToFieldPath(error.pointer)}>
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
