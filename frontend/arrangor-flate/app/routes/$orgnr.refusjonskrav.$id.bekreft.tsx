import { Alert, Button, Checkbox, ErrorSummary, TextField, VStack } from "@navikt/ds-react";
import { ActionFunction, json, LoaderFunction, redirect } from "@remix-run/node";
import { Form, useActionData, useLoaderData } from "@remix-run/react";
import { PageHeader } from "~/components/PageHeader";
import { Refusjonskrav } from "~/domene/domene";
import { checkValidToken } from "~/auth/auth.server";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client";
import { RefusjonskravDetaljer } from "~/components/refusjonskrav/RefusjonskravDetaljer";
import { useOrgnrFromUrl } from "../utils";
import { internalNavigation } from "../internal-navigation";
import { Definisjon } from "~/components/Definisjon";

type BekreftRefusjonskravData = {
  krav: Refusjonskrav;
  tilsagn: ArrangorflateTilsagn[];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<BekreftRefusjonskravData> => {
  await checkValidToken(request);

  const { id, orgnr } = params;
  if (!id || !orgnr) {
    throw Error("Mangler id eller orgnr");
  }

  const [krav, tilsagn] = await Promise.all([
    loadRefusjonskrav(id, orgnr),
    ArrangorflateService.getArrangorflateTilsagnTilRefusjon({ id, orgnr }),
  ]);

  return { krav, tilsagn };
};

export const action: ActionFunction = async ({ request }) => {
  const formdata = await request.formData();
  const bekreftelse = formdata.get("bekreftelse");
  const refusjonskravId = formdata.get("refusjonskravId")?.toString();
  const kontonummer = formdata.get("kontonummer");
  const kid = formdata.get("kid");
  const orgnr = formdata.get("orgnr")?.toString();

  const errors: { [key: string]: string } = {};

  if (!bekreftelse) {
    errors.bekreftelse = "Du må bekrefte at opplysningene er korrekte";
  }

  if (!kontonummer) {
    errors.kontonummer = "Du må fylle ut kontonummer";
  }
  if (!refusjonskravId) {
    throw new Error("Mangler refusjonskravId");
  }

  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }

  if (Object.keys(errors).length > 0) {
    return json({ errors });
  }

  await ArrangorflateService.godkjennRefusjonskrav({
    id: refusjonskravId as string,
    orgnr,
    requestBody: {
      kontonummer: kontonummer as string,
      kid: kid as string,
    },
  });

  return redirect(internalNavigation(orgnr).kvittering(refusjonskravId));
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
          url: `/${orgnr}/refusjonskrav/${krav.id}/beregning`,
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
              <span>{krav.betalingsinformasjon.kid}</span>
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
            <input type="hidden" name="orgnr" value={orgnr} />
            {data?.errors
              ? Object.keys(data?.errors)?.length > 0 && (
                  <ErrorSummary>
                    {Object.values(data?.errors).map((error, index) => {
                      return <ErrorSummary.Item key={index}>{error as any}</ErrorSummary.Item>;
                    })}
                  </ErrorSummary>
                )
              : null}
            {data?.error && <Alert variant="error">{data.error}</Alert>}
            <Button type="submit">Bekreft og send refusjonskrav</Button>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
