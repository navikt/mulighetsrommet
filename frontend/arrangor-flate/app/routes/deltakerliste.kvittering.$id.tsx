import { Alert, VStack } from "@navikt/ds-react";
import { LoaderFunction } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
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

export default function RefusjonskravKvittering() {
  const { deltakerliste } = useLoaderData<LoaderData>();
  return (
    <>
      <PageHeader
        title="Kvittering"
        tilbakeLenke={{
          navn: "Tilbake til refusjonskravliste",
          url: `/`,
        }}
      />
      <VStack gap="5">
        <DeltakerlisteDetaljer deltakerliste={deltakerliste} />
        <Alert variant="info">Her kommer info på kvitteringssiden</Alert>
      </VStack>
    </>
  );
}
