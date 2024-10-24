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
import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";

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
  const kontoNummer = formdata.get("kontoNummer");
  const kid = formdata.get("kid");

  if (!bekreftelse) {
    return json({ error: "Du må bekrefte at opplysningene er korrekte" }, { status: 400 });
  }

  if (!refusjonskravId) {
    return json({ error: "Mangler refusjonskravId" }, { status: 400 });
  }

  if (!kontoNummer) {
    return json({ error: "Mangler kontonummer" }, { status: 400 });
  }

  await ArrangorflateService.godkjennRefusjonskrav({
    id: refusjonskravId as string,
  });

  await ArrangorflateService.setRefusjonskravBetalingsinformasjon({
    id: refusjonskravId as string,
    requestBody: {
      kontoNummer: kontoNummer as string,
      kid: kid as string,
    },
  });

  return redirect(`/deltakerliste/kvittering/${refusjonskravId}`);
};

export default function RefusjonskravDetaljer() {
  const { tilsagn, krav } = useLoaderData<LoaderData>();
  const data = useActionData<typeof action>();
  const [isEditing, setIsEditing] = React.useState(false);
  const [kontoNummer, setKontoNummer] = React.useState(krav.betalingsinformasjon.kontoNummer);

  return (
    <>
      <PageHeader
        title="Detaljer for refusjonskrav"
        tilbakeLenke={{
          navn: "Tilbake til deltakerliste",
          url: `/deltakerliste/${krav.id}`,
        }}
      />
      <VStack className="max-w-[70%]" gap="5">
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

        <Form method="post">
          <Definisjon label="Kontonummer">
            <input
              type="text"
              name="kontoNummer"
              className={"border border-[#0214317D] rounded-md " + (isEditing ? "" : "hidden")}
              value={kontoNummer}
              onChange={(e) => setKontoNummer(e.target.value)}
            />
            <span className={"ml-4 cursor-pointer " + (isEditing ? "hidden" : "")}>
              {formaterKontoNummer(kontoNummer)}
            </span>
            <span
              className="ml-4 text-text-action cursor-pointer"
              onClick={() => {
                setIsEditing(!isEditing);
              }}
            >
              Endre
            </span>
          </Definisjon>
          <Definisjon label="Evt KID nr for refusjonskrav" className="my-4 flex">
            <div className="flex">
              <span>{krav.betalingsinformasjon.kid}</span>
              <input type="text" name="kid" className="border border-[#0214317D] rounded-md" />
            </div>
          </Definisjon>
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
