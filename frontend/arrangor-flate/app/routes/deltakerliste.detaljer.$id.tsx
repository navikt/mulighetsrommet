import { Alert, Box, Button, Checkbox, Heading, VStack } from "@navikt/ds-react";
import { ActionFunction, json, LoaderFunction, redirect } from "@remix-run/node";
import { Form, useActionData, useLoaderData } from "@remix-run/react";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { PageHeader } from "../components/PageHeader";
import { Refusjonskrav } from "../domene/domene";
import { checkValidToken } from "../auth/auth.server";
import { RefusjonDetaljer } from "~/components/refusjonskrav/RefusjonDetaljer";
import { Separator } from "~/components/Separator";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import React from "react";
import { Definisjon } from "~/components/Definisjon";

type LoaderData = {
  krav: Refusjonskrav;
  tilsagn: ArrangorflateTilsagn[];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);

  if (params.id === undefined) throw Error("Mangler id");

  const krav = await loadRefusjonskrav(params.id);
  const tilsagn = await ArrangorflateService.getArrangorflateTilsagnTilRefusjon({ id: krav.id });

  return {
    krav,
    tilsagn,
  };
};

export const action: ActionFunction = async ({ request }) => {
  const formdata = await request.formData();
  const bekreftelse = formdata.get("bekreftelse");
  const refusjonskravId = formdata.get("refusjonskravId");

  if (!bekreftelse) {
    return json({ error: "Du må bekrefte at opplysningene er korrekte" }, { status: 400 });
  }

  if (!refusjonskravId) {
    return json({ error: "Mangler refusjonskravId" }, { status: 400 });
  }

  await ArrangorflateService.godkjennRefusjonskrav({
    id: refusjonskravId as string,
  });

  return redirect(`/deltakerliste/kvittering/${refusjonskravId}`);
};

export default function RefusjonskravDetaljer() {
  const { tilsagn, krav } = useLoaderData<LoaderData>();
  const data = useActionData<typeof action>();

  return (
    <>
      <PageHeader
        title="Detaljer for refusjonskrav"
        tilbakeLenke={{
          navn: "Tilbake til deltakerliste",
          url: `/deltakerliste/${krav.id}`,
        }}
      />
      <VStack className="max-w-[50%]" gap="5">
        <DeltakerlisteDetaljer krav={krav} />
        <Separator />
        <Heading size="medium">Tilsagnsdetaljer</Heading>
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
        <RefusjonDetaljer krav={krav} />
        <Separator />
        <Heading className="mb-2" size="medium" level="2">
          Betalingsinformasjon
        </Heading>
        <Definisjon key="Kontonummer" value={""} />

        <Form method="post">
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox name="bekreftelse" value="bekreftet">
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
            <input type="hidden" name="refusjonskravId" value={krav.id} />
            {data?.error && <Alert variant="error">{data.error}</Alert>}
            <Button type="submit">Bekreft og send refusjonskrav</Button>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
