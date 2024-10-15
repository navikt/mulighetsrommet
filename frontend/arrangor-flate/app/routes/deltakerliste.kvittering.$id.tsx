import { FilePdfIcon } from "@navikt/aksel-icons";
import { Button, VStack } from "@navikt/ds-react";
import { LoaderFunction } from "@remix-run/node";
import { useLoaderData, useParams } from "@remix-run/react";
import { Definisjonsliste } from "../components/Definisjonsliste";
import { PageHeader } from "../components/PageHeader";
import { Separator } from "../components/Separator";
import { Refusjonskrav, TilsagnDetaljer } from "../domene/domene";
import { checkValidToken } from "../auth/auth.server";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { DeltakerlisteDetaljer } from "~/components/deltakerliste/DeltakerlisteDetaljer";
import { RefusjonTilsagnsDetaljer } from "~/components/refusjonskrav/TilsagnsDetaljer";
import { RefusjonDetaljer } from "~/components/refusjonskrav/RefusjonDetaljer";

type LoaderData = {
  krav: Refusjonskrav;
  tilsagnDetaljer: TilsagnDetaljer;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);

  if (params.id === undefined) throw Error("Mangler id");

  const krav = await loadRefusjonskrav(params.id);

  return {
    krav,
    tilsagnDetaljer: {
      antallPlasser: 20,
      prisPerPlass: 20205,
      tilsagnsBelop: 1308530,
      tilsagnsPeriode: "01.06.2024 - 30.06.2024",
      sum: 1308530,
    },
  };
};

export default function RefusjonskravKvittering() {
  const { krav, tilsagnDetaljer } = useLoaderData<LoaderData>();
  const params = useParams();
  return (
    <>
      <PageHeader
        title="Kvittering"
        tilbakeLenke={{
          navn: "Tilbake til refusjonskravliste",
          url: `/`,
        }}
      />
      <Separator />
      <div className="flex justify-end">
        <a
          href={`${import.meta.env.VITE_MULIGHETSROMMET_API_BASE}/api/v1/intern/refusjon/kvittering/${params.id}`}
        >
          <Button variant="tertiary-neutral" size="small">
            <span className="flex gap-2 items-center">
              Last ned som PDF <FilePdfIcon fontSize={35} />
            </span>
          </Button>
        </a>
      </div>
      <Separator />
      <VStack gap="5" className="mt-5">
        <DeltakerlisteDetaljer krav={krav} />
        <Separator />
        <RefusjonTilsagnsDetaljer tilsagnsDetaljer={tilsagnDetaljer} />
        <Separator />
        <RefusjonDetaljer krav={krav} />
        <Separator />
        <Definisjonsliste
          title="Betalingsinformasjon"
          definitions={[
            { key: "Kontonummer", value: "1234.56.78901" },
            { key: "KID-nummer", value: "123456701123453" },
            { key: "Refusjonskravansvarlig", value: "Ingvild Pettersen" },
            { key: "E-postadresse", value: "ingvild.pettersen@fretexas.no" },
          ]}
        />
        <VStack align={"start"}>
          <Button className="" as="a" href="/" variant="secondary">
            Tilbake til refusjonskravliste
          </Button>
        </VStack>
      </VStack>
    </>
  );
}
