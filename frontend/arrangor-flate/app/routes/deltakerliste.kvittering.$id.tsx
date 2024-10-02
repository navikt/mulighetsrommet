import { FilePdfIcon } from "@navikt/aksel-icons";
import { Button, VStack } from "@navikt/ds-react";
import { LoaderFunction } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { Definisjonsliste } from "../components/Definisjonsliste";
import { PageHeader } from "../components/PageHeader";
import { Separator } from "../components/Separator";
import { Deltakerliste, TilsagnsDetaljer } from "../domene/domene";
import { requirePersonIdent } from "../auth/auth.server";
import { DeltakerlisteDetaljer } from "~/components/deltakerliste/DeltakerlisteDetaljer";
import { RefusjonTilsagnsDetaljer } from "~/components/refusjonskrav/TilsagnsDetaljer";

type LoaderData = {
  deltakerliste: Deltakerliste;
  tilsagnsDetaljer: TilsagnsDetaljer;
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
  };
};

export default function RefusjonskravKvittering() {
  const { deltakerliste, tilsagnsDetaljer } = useLoaderData<LoaderData>();
  const { tiltaksnavn, tiltaksnummer, avtalenavn, tiltakstype } = deltakerliste.detaljer;
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
        <Button
          variant="tertiary-neutral"
          size="small"
          onClick={() => alert("Nedlasting av pdf er ikke støttet enda")}
        >
          <span className="flex gap-2 items-center">
            Last ned som PDF <FilePdfIcon fontSize={35} />
          </span>
        </Button>
      </div>
      <Separator />
      <VStack gap="5" className="mt-5">
        <DeltakerlisteDetaljer deltakerliste={deltakerliste} />
        <Separator />
        <RefusjonTilsagnsDetaljer tilsagnsDetaljer={tilsagnsDetaljer} />
        <Separator />
        <Definisjonsliste
          title="Refusjonskrav"
          definitions={[
            { key: "Refusjonskravnummer", value: "6" },
            { key: "Refusjonskravperiode", value: "01.06.2024 - 30.06.2024" },
            { key: "Antall månedsverk", value: "15.27" },
            { key: "Totalt refusjonskrav", value: "kr 308 530" },
            { key: "Gjenstående beløp på tilsagnet", value: "kr 291 470" },
          ]}
        />
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
