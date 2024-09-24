import { Alert, Button, HGrid } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { Link, useLoaderData } from "@remix-run/react";
import { PageHeader } from "../components/PageHeader";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { Deltakerliste } from "../domene/domene";
import { requirePersonIdent } from "../auth/auth.server";

export const meta: MetaFunction = () => {
  return [{ title: "Refusjon" }, { name: "description", content: "Refusjonsdetaljer" }];
};

type LoaderData = {
  deltakerliste: Deltakerliste;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await requirePersonIdent(request);
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

export default function RefusjonDeltakerlister() {
  const { deltakerliste } = useLoaderData<LoaderData>();
  return (
    <>
      <PageHeader
        title="Deltakerliste"
        tilbakeLenke={{ navn: "Tilbake til refusjonsliste", url: "/" }}
      />
      <HGrid gap="5" columns={1}>
        <DeltakerlisteDetaljer deltakerliste={deltakerliste} />
        <Alert variant="info">Her kommer deltakertabell</Alert>
        <Button
          as={Link}
          className="justify-self-end"
          to={`/deltakerliste/detaljer/${deltakerliste.id}`}
        >
          Neste
        </Button>
      </HGrid>
    </>
  );
}
