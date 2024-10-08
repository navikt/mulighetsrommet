import { Alert, Button, Checkbox, VStack } from "@navikt/ds-react";
import { ActionFunction, LoaderFunction, redirect, json } from "@remix-run/node";
import { Form, useActionData, useLoaderData } from "@remix-run/react";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { PageHeader } from "../components/PageHeader";
import { Deltakerliste, Krav, KravStatus, type TilsagnsDetaljer } from "../domene/domene";
import { requirePersonIdent } from "../auth/auth.server";
import Divider from "node_modules/@navikt/ds-react/esm/dropdown/Menu/Divider";
import { RefusjonTilsagnsDetaljer } from "~/components/refusjonskrav/TilsagnsDetaljer";
import { RefusjonDetaljer } from "~/components/refusjonskrav/RefusjonDetaljer";
import { RefusjonskravService } from "@mr/api-client";
import { Separator } from "~/components/Separator";

type LoaderData = {
  deltakerliste: Deltakerliste;
  tilsagnsDetaljer: TilsagnsDetaljer;
  krav: Krav;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await requirePersonIdent(request);
  if (params.id === undefined) throw Error("Mangler id");
  const krav = await RefusjonskravService.getRefusjonkrav({
    id: params.id,
  });

  return {
    deltakerliste: {
      id: params.id,
      detaljer: {
        tiltaksnavn: krav.tiltaksgjennomforing.navn,
        tiltaksnummer: krav.tiltaksgjennomforing.tiltaksnummer,
        avtalenavn: krav.avtale.navn,
        tiltakstype: krav.tiltakstype.navn,
        refusjonskravperiode: `${krav.periodeStart} - ${krav.periodeSlutt}`,
      },
      deltakere: [],
    },
    tilsagnsDetaljer: {
      antallPlasser: 20,
      prisPerPlass: 20205,
      tilsagnsBelop: 1308530,
      tilsagnsPeriode: "01.06.2024 - 30.06.2024",
      sum: 1308530,
    },
    krav: {
      id: krav.id,
      kravnr: "6",
      periode: `${krav.periodeStart} - ${krav.periodeSlutt}`,
      belop: String(krav.beregning.belop),
      fristForGodkjenning: "01.02.2024",
      tiltaksnr: krav.tiltaksgjennomforing.tiltaksnummer!,
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
  const { deltakerliste, tilsagnsDetaljer, krav } = useLoaderData<LoaderData>();
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
            <input type="hidden" name="deltakerlisteId" value={deltakerliste.id} />
            {data?.error && <Alert variant="error">{data.error}</Alert>}
            <Button type="submit">Bekreft og send refusjonskrav</Button>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
