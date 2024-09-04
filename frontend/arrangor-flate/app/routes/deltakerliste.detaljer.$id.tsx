import { Alert, Button, Checkbox, VStack } from "@navikt/ds-react";
import { ActionFunction, LoaderFunction } from "@remix-run/node";
import { Form, json, redirect, useActionData, useLoaderData } from "@remix-run/react";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { PageHeader } from "../components/PageHeader";
import { Deltakerliste } from "../domene/domene";

type LoaderData = {
  deltakerliste: Deltakerliste;
};

export const loader: LoaderFunction = async ({ params }): Promise<LoaderData> => {
  if (params.id === undefined) throw Error("Mangler id");
  return {
    deltakerliste: {
      id: params.id,
      detaljer: {
        tiltaksnavn: "AFT - Fredrikstad, Sarpsborg, Halden",
        tiltaksnummer: "2024/123456",
        avtalenavn: "AFT - Fredrikstad, Sarpsborg, Halden",
        tiltakstype: "Arbeidsforberedende trening",
        refusjonskravperiode: "01.01.2024 - 31.01.2024",
        refusjonskravnummer: "6",
      },
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
  const { deltakerliste } = useLoaderData<LoaderData>();
  const data = useActionData<typeof action>();
  return (
    <>
      <PageHeader
        title="Detaljer for refusjonskrav"
        tilbakeLenke={{
          navn: "Tilbake til deltakerliste",
          url: `/deltakerliste/${deltakerliste.id}`,
        }}
      />
      <VStack gap="5">
        <DeltakerlisteDetaljer deltakerliste={deltakerliste} />
        <Alert variant="info">Her kommer tilsagnsdetaljer</Alert>
        <Alert variant="info">Her kommer info om refusjonskrav</Alert>
        <Form method="post">
          <VStack gap="2" justify={"start"} align={"start"}>
            <Checkbox name="bekreftelse" value="bekreftet">
              Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold
            </Checkbox>
            <input type="hidden" name="deltakerlisteId" value={deltakerliste.id} />
            {data?.error && <Alert variant="error">{data.error}</Alert>}
            <Button type="submit">Bekreft og send refusjonskrav</Button>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
