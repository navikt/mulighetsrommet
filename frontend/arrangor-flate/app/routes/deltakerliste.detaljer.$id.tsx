import { Alert, Button, Checkbox, VStack } from "@navikt/ds-react";
import { ActionFunction, json, LoaderFunction, redirect } from "@remix-run/node";
import { Form, useActionData, useLoaderData } from "@remix-run/react";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { PageHeader } from "../components/PageHeader";
import { Refusjonskrav, type TilsagnDetaljer } from "../domene/domene";
import { requirePersonIdent } from "../auth/auth.server";
import { RefusjonTilsagnsDetaljer } from "~/components/refusjonskrav/TilsagnsDetaljer";
import { RefusjonDetaljer } from "~/components/refusjonskrav/RefusjonDetaljer";
import { Separator } from "~/components/Separator";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { RefusjonskravService } from "@mr/api-client";

type LoaderData = {
  krav: Refusjonskrav;
  tilsagnsDetaljer: TilsagnDetaljer;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await requirePersonIdent(request);

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
  const refusjonskravId = formdata.get("refusjonskravId");

  if (!bekreftelse) {
    return json({ error: "Du må bekrefte at opplysningene er korrekte" }, { status: 400 });
  }

  if (!refusjonskravId) {
    return json({ error: "Mangler refusjonskravId" }, { status: 400 });
  }

  await RefusjonskravService.godkjennRefusjonskrav({
    id: refusjonskravId as string,
  });

  return redirect(`/deltakerliste/kvittering/${refusjonskravId}`);
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
