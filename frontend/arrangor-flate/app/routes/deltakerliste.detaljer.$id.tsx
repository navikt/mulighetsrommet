import { Alert, Button, Checkbox, VStack } from "@navikt/ds-react";
import { ActionFunction, json, LoaderFunction, redirect } from "@remix-run/node";
import { Form, useActionData, useLoaderData } from "@remix-run/react";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { PageHeader } from "../components/PageHeader";
import { Refusjonskrav, type TilsagnDetaljer } from "../domene/domene";
import { checkValidToken } from "../auth/auth.server";
import { RefusjonTilsagnsDetaljer } from "~/components/refusjonskrav/TilsagnsDetaljer";
import { RefusjonDetaljer } from "~/components/refusjonskrav/RefusjonDetaljer";
import { Separator } from "~/components/Separator";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";

type LoaderData = {
  krav: Refusjonskrav;
  tilsagnsDetaljer: TilsagnDetaljer;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);

  if (params.id === undefined) throw Error("Mangler id");

  const krav = await loadRefusjonskrav(params.id);

  return {
    krav,
    tilsagnsDetaljer: {
      antallPlasser: 20,
      prisPerPlass: 20205,
      tilsagnsBelop: 1308530,
      tilsagnsPeriode: "01.06.2024 - 30.06.2024",
      sum: 1308530,
    },
  };
};

export const action: ActionFunction = async ({ request }) => {
  const formdata = await request.formData();
  const bekreftelse = formdata.get("bekreftelse");
  const deltakerlisteId = formdata.get("deltakerlisteId");

  if (!bekreftelse) {
    return json({ error: "Du må bekrefte at opplysningene er korrekte" }, { status: 400 });
  }

  return redirect(`/deltakerliste/kvittering/${deltakerlisteId}`);
};

export default function RefusjonskravDetaljer() {
  const { tilsagnsDetaljer, krav } = useLoaderData<LoaderData>();
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
      <VStack gap="5">
        <DeltakerlisteDetaljer krav={krav} />
        <Separator />
        <RefusjonTilsagnsDetaljer tilsagnsDetaljer={tilsagnsDetaljer} />
        <Separator />
        <RefusjonDetaljer krav={krav} />

        <Alert variant="info">Her kommer tilsagnsdetaljer</Alert>
        <Alert variant="info">Her kommer info om refusjonskrav</Alert>
        <Form method="post">
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox name="bekreftelse" value="bekreftet">
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
            <input type="hidden" name="deltakerlisteId" value={krav.id} />
            {data?.error && <Alert variant="error">{data.error}</Alert>}
            <Button type="submit">Bekreft og send refusjonskrav</Button>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
