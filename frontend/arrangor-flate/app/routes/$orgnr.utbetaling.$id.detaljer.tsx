import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { FilePdfIcon } from "@navikt/aksel-icons";
import { Box, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { ArrangorflateService, ArrFlateUtbetaling } from "api-client";
import { LoaderFunction, MetaFunction, useLoaderData, useParams } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeader } from "~/components/PageHeader";
import { Separator } from "~/components/Separator";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import { Definisjonsliste } from "../components/Definisjonsliste";
import { internalNavigation } from "../internal-navigation";
import { tekster } from "../tekster";
import { formaterDato, formaterPeriode, problemDetailResponse, useOrgnrFromUrl } from "../utils";
import { getBeregningDetaljer } from "../utils/beregning";

type UtbetalingDetaljerSideData = {
  utbetaling: ArrFlateUtbetaling;
};

interface TimestampInfo {
  title: string;
  value: string;
}

const getTimestamp = (utbetaling: ArrFlateUtbetaling): TimestampInfo => {
  if (utbetaling.godkjentAvArrangorTidspunkt) {
    return {
      title: "Dato innsendt",
      value: formaterDato(utbetaling.godkjentAvArrangorTidspunkt),
    };
  }

  return {
    title: "Dato opprettet hos Nav",
    value: utbetaling.createdAt ? formaterDato(utbetaling.createdAt) : "-",
  };
};

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetaling | Detaljer" },
    { name: "description", content: "Arrangørflate for detaljer om en utbetaling" },
  ];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<UtbetalingDetaljerSideData> => {
  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const [{ data: utbetaling, error: utbetalingError }] = await Promise.all([
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }

  return { utbetaling };
};

export default function UtbetalingDetaljerSide() {
  const { utbetaling } = useLoaderData<UtbetalingDetaljerSideData>();
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  const innsendtTidspunkt = getTimestamp(utbetaling);

  return (
    <>
      <PageHeader
        title="Detaljer"
        tilbakeLenke={{
          navn: tekster.bokmal.tilbakeTilOversikt,
          url: internalNavigation(orgnr).utbetalinger,
        }}
      />

      <HStack gap="2" className="max-w-[1250px] mt-5" justify="space-between">
        <VStack gap="2">
          <Heading level="2" size="medium">
            Innsending
          </Heading>
          <Definisjonsliste
            className="mb-3"
            headingLevel="3"
            definitions={[
              {
                key: innsendtTidspunkt.title,
                value: innsendtTidspunkt.value,
              },
            ]}
          />
        </VStack>
        <a href={`/${orgnr}/utbetaling/${id}/kvittering/lastned`} target="_blank">
          <Button variant="tertiary-neutral" size="small">
            <span className="flex gap-2 items-center">
              Last ned som PDF <FilePdfIcon fontSize={35} />
            </span>
          </Button>
        </a>
      </HStack>
      <Separator />

      <VStack gap="6" className="max-w-[1250px] mt-5">
        <Definisjonsliste
          definitions={[
            { key: "Tiltaksnavn", value: utbetaling.gjennomforing.navn },
            { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
          ]}
        />

        <Definisjonsliste
          title={"Utbetaling"}
          headingLevel="3"
          definitions={[
            {
              key: "Utbetalingsperiode",
              value: formaterPeriode(utbetaling.periode),
            },
            ...getBeregningDetaljer(utbetaling.beregning),
          ]}
        />
        <Definisjonsliste
          title="Betalingsinformasjon"
          headingLevel="3"
          definitions={[
            {
              key: "Kontonummer",
              value: utbetaling.betalingsinformasjon.kontonummer
                ? formaterKontoNummer(utbetaling.betalingsinformasjon.kontonummer)
                : "-",
            },
            {
              key: "KID-nummer",
              value: utbetaling.betalingsinformasjon.kid || "-",
            },
          ]}
        />
        <Box
          background="bg-subtle"
          padding="6"
          borderRadius="medium"
          borderColor="border-subtle"
          borderWidth={"2 1 1 1"}
        >
          <VStack gap="6">
            <UtbetalingStatusList utbetaling={utbetaling} />
          </VStack>
        </Box>
      </VStack>
    </>
  );
}
