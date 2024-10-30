import { Alert, Button, Checkbox, VStack } from "@navikt/ds-react";
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
import invariant from "tiny-invariant";
import React from "react";
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

  if (params.id === undefined) {
    throw Error("Mangler id");
  }

  const [krav, tilsagn] = await Promise.all([
    loadRefusjonskrav(params.id),
    ArrangorflateService.getArrangorflateTilsagnTilRefusjon({ id: params.id }),
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

  if (!bekreftelse) {
    return json({ error: "Du må bekrefte at opplysningene er korrekte" }, { status: 400 });
  }

  if (!refusjonskravId) {
    return json({ error: "Mangler refusjonskravId" }, { status: 400 });
  }

  if (!kontonummer) {
    return json({ error: "Mangler kontonummer" }, { status: 400 });
  }

  invariant(orgnr, "Mangler orgnr");

  await ArrangorflateService.godkjennRefusjonskrav({
    id: refusjonskravId as string,
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
        title="Detaljer for refusjonskrav"
        tilbakeLenke={{
          navn: "Tilbake til deltakerliste",
          url: `/refusjonskrav/${orgnr}/${krav.id}`,
        }}
      />
      <VStack className="max-w-[50%]" gap="5">
        <RefusjonskravDetaljer krav={krav} tilsagn={tilsagn} />

        <Form method="post">
          <Definisjon label="Kontonummer">
            <input
              type="text"
              name="kontonummer"
              className="border border-[#0214317D] rounded-md"
              defaultValue={krav.betalingsinformasjon?.kontonummer}
              maxLength={11}
            />
          </Definisjon>
          <Definisjon label="Evt KID nr for refusjonskrav" className="my-4 flex">
            <div className="flex">
              <span>{krav.betalingsinformasjon.kid}</span>
              <input
                type="text"
                name="kid"
                className="border border-[#0214317D] rounded-md"
                defaultValue={krav.betalingsinformasjon?.kid}
                maxLength={25}
              />
            </div>
          </Definisjon>
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox name="bekreftelse" value="bekreftet">
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
            <input type="hidden" name="refusjonskravId" value={krav.id} />
            <input type="hidden" name="orgnr" value={orgnr} />
            {data?.error && <Alert variant="error">{data.error}</Alert>}
            <Button type="submit">Bekreft og send refusjonskrav</Button>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
