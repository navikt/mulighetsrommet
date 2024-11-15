import { ApiError, ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client";
import { Alert, Button, Checkbox, ErrorSummary, TextField, VStack } from "@navikt/ds-react";
import { ActionFunction, json, LoaderFunction, redirect } from "@remix-run/node";
import { Form, useActionData, useLoaderData } from "@remix-run/react";
import { checkValidToken } from "~/auth/auth.server";
import { Definisjon } from "~/components/Definisjon";
import { PageHeader } from "~/components/PageHeader";
import { RefusjonskravDetaljer } from "~/components/refusjonskrav/RefusjonskravDetaljer";
import { Refusjonskrav } from "~/domene/domene";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { internalNavigation } from "~/internal-navigation";
import { useOrgnrFromUrl } from "~/utils";
import { getCurrentTab } from "~/utils/currentTab";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { FormError, getOrError, getOrThrowError } from "~/form/form-helpers";

type BekreftRefusjonskravData = {
  krav: Refusjonskrav;
  tilsagn: ArrangorflateTilsagn[];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<BekreftRefusjonskravData> => {
  await checkValidToken(request);

  const { id } = params;
  if (!id) {
    throw Error("Mangler id");
  }

  const [krav, tilsagn] = await Promise.all([
    loadRefusjonskrav(id),
    ArrangorflateService.getArrangorflateTilsagnTilRefusjon({ id }),
  ]);

  return { krav, tilsagn };
};

export const action: ActionFunction = async ({ request }) => {
  const formData = await request.formData();

  const currentTab = getCurrentTab(request);
  const refusjonskravId = getOrThrowError(formData, "refusjonskravId").toString();
  const refusjonskravDigest = getOrThrowError(formData, "refusjonskravDigest").toString();
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
    return json({
      errors: [kontonummerError, bekreftelseError].filter((error) => error !== undefined),
    });
  }

  try {
    await ArrangorflateService.godkjennRefusjonskrav({
      id: refusjonskravId,
      requestBody: {
        digest: refusjonskravDigest,
        betalingsinformasjon: {
          kontonummer: kontonummer.toString(),
          kid: kid,
        },
      },
    });

    return redirect(
      `${internalNavigation(orgnr).kvittering(refusjonskravId)}?forside-tab=${currentTab}`,
    );
  } catch (e) {
    const apiError = e as ApiError;
    if (apiError.status === 400 && isValidationError(apiError.body)) {
      // Remix revaliderer loader data ved actions, så når denne feilmeldingen vises skal allerede kravet
      // være oppdatert. Det kan hende vi i fremtiden vil vise _hva_ som har endret seg også, men det
      // får vi ta senere.
      return json({ errors: apiError.body.errors });
    }
    throw e;
  }
};

export default function BekreftRefusjonskrav() {
  const { krav, tilsagn } = useLoaderData<BekreftRefusjonskravData>();
  const data = useActionData<typeof action>();
  const orgnr = useOrgnrFromUrl();
  return (
    <>
      <PageHeader
        title="Oppsummering av refusjonskrav"
        tilbakeLenke={{
          navn: "Tilbake til beregning",
          url: internalNavigation(orgnr).beregning(krav.id),
        }}
      />
      <VStack className="max-w-[50%]" gap="5">
        <RefusjonskravDetaljer krav={krav} tilsagn={tilsagn} />
        <Form method="post">
          <Definisjon label="Kontonummer">
            <TextField
              label="Kontonummer"
              hideLabel
              size="small"
              error={data?.errors?.kontonummer}
              name="kontonummer"
              className="border border-[#0214317D] rounded-md"
              defaultValue={krav.betalingsinformasjon?.kontonummer}
              maxLength={11}
              minLength={11}
            />
          </Definisjon>
          <Definisjon label="Evt KID nr for refusjonskrav" className="my-4 flex">
            <div className="flex">
              <TextField
                label="Evt KID nr for refusjonskrav"
                hideLabel
                size="small"
                name="kid"
                className="border border-[#0214317D] rounded-md"
                defaultValue={krav.betalingsinformasjon?.kid}
                maxLength={25}
              />
            </div>
          </Definisjon>
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox name="bekreftelse" value="bekreftet" error={data?.errors?.bekreftelse}>
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
            <input type="hidden" name="refusjonskravId" value={krav.id} />
            <input type="hidden" name="refusjonskravDigest" value={krav.beregning.digest} />
            <input type="hidden" name="orgnr" value={orgnr} />
            {data?.errors?.length > 0 && (
              <ErrorSummary>
                {data.errors.map((error: FormError) => {
                  return <ErrorSummary.Item key={error.name}>{error.message}</ErrorSummary.Item>;
                })}
              </ErrorSummary>
            )}
            {data?.error && <Alert variant="error">{data.error}</Alert>}
            <Button type="submit">Bekreft og send refusjonskrav</Button>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
