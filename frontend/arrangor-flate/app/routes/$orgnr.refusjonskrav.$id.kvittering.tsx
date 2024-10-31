import { FilePdfIcon } from "@navikt/aksel-icons";
import { Button, VStack } from "@navikt/ds-react";
import { LoaderFunction } from "@remix-run/node";
import { useLoaderData, useParams } from "@remix-run/react";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { PageHeader } from "~/components/PageHeader";
import { Separator } from "~/components/Separator";
import { Refusjonskrav } from "~/domene/domene";
import { checkValidToken } from "~/auth/auth.server";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client";
import { RefusjonskravDetaljer } from "~/components/refusjonskrav/RefusjonskravDetaljer";
import { internalNavigation } from "../internal-navigation";
import { useOrgnrFromUrl } from "../utils";

type RefusjonskavKvitteringData = {
  krav: Refusjonskrav;
  tilsagn: ArrangorflateTilsagn[];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<RefusjonskavKvitteringData> => {
  await checkValidToken(request);

  if (params.id === undefined) {
    throw Error("Mangler id");
  }

  const [krav, tilsagn] = await Promise.all([
    loadRefusjonskrav(params.id),
    ArrangorflateService.getArrangorflateTilsagnTilRefusjon({ id: params.id }),
  ]);

  return { krav, tilsagn };
};

export default function RefusjonskravKvittering() {
  const { krav, tilsagn } = useLoaderData<RefusjonskavKvitteringData>();
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <PageHeader
        title="Kvittering"
        tilbakeLenke={{
          navn: "Tilbake til refusjonskravliste",
          url: internalNavigation(orgnr).refusjonskravliste,
        }}
      />
      <Separator />
      <div className="flex justify-end">
        <a href={`/refusjonskrav/${id}/kvittering/lastned`} target="_blank">
          <Button variant="tertiary-neutral" size="small">
            <span className="flex gap-2 items-center">
              Last ned som PDF <FilePdfIcon fontSize={35} />
            </span>
          </Button>
        </a>
      </div>
      <Separator />
      <VStack gap="5" className="max-w-[50%] mt-5">
        <RefusjonskravDetaljer krav={krav} tilsagn={tilsagn} />
        <Separator />
        <Definisjonsliste
          title="Betalingsinformasjon"
          definitions={[
            {
              key: "Kontonummer",
              value: formaterKontoNummer(krav.betalingsinformasjon.kontonummer),
            },
            {
              key: "KID-nummer",
              value: krav.betalingsinformasjon.kid!,
            },
            { key: "Refusjonskravansvarlig", value: "Ingvild Pettersen" },
            { key: "E-postadresse", value: "ingvild.pettersen@fretexas.no" },
          ]}
        />
        <VStack align={"start"}>
          <Button
            className=""
            as="a"
            href={internalNavigation(orgnr).refusjonskravliste}
            variant="secondary"
          >
            Tilbake til refusjonskravliste
          </Button>
        </VStack>
      </VStack>
    </>
  );
}