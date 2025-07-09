import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { FilePdfIcon } from "@navikt/aksel-icons";
import { Box, Spacer, HStack, VStack, Link } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrFlateUtbetaling,
  ArrFlateUtbetalingStatus,
  UtbetalingType,
} from "api-client";
import { LoaderFunction, MetaFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import { Definisjonsliste } from "../components/common/Definisjonsliste";
import { tekster } from "../tekster";
import { getBeregningDetaljer } from "../utils/beregning";
import css from "../root.module.css";
import { UtbetalingTypeText } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterPeriode, getTimestamp } from "~/utils/date";
import { problemDetailResponse } from "~/utils/validering";
import { pathByOrgnr } from "~/utils/navigation";
import { PageHeading } from "~/components/common/PageHeading";

type UtbetalingDetaljerSideData = {
  utbetaling: ArrFlateUtbetaling;
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

  const innsendtTidspunkt = getTimestamp(utbetaling);

  const innsendingHeader = (type: UtbetalingType | undefined) => {
    if (type === UtbetalingType.KORRIGERING) {
      return <UtbetalingTypeText type={type} text={"Korrigering"} />;
    }
    if (type === UtbetalingType.INVESTERING) {
      return <UtbetalingTypeText type={type} text={"Utbetaling for investering"} />;
    }
    return "Innsending";
  };

  const visNedlastingAvKvittering = [
    ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    ArrFlateUtbetalingStatus.UTBETALT,
  ].includes(utbetaling.status);

  return (
    <VStack gap="4" className={css.side}>
      <HStack gap="2" className="max-w-[1250px]" align="end" justify="space-between">
        <PageHeading
          title="Detaljer"
          tilbakeLenke={{
            navn: tekster.bokmal.tilbakeTilOversikt,
            url: pathByOrgnr(utbetaling.arrangor.organisasjonsnummer).utbetalinger,
          }}
        />
        <Spacer />
        {visNedlastingAvKvittering && (
          <Link
            href={`/${utbetaling.arrangor.organisasjonsnummer}/utbetaling/${utbetaling.id}/detaljer/lastned?filename=${tekster.bokmal.utbetaling.pdfNavn(utbetaling.periode.start)}`}
            target="_blank"
          >
            <FilePdfIcon />
            Last ned som PDF (Åpner i ny fane)
          </Link>
        )}
      </HStack>
      {innsendingHeader(utbetaling.type)}
      <Definisjonsliste
        definitions={[
          {
            key: innsendtTidspunkt.title,
            value: innsendtTidspunkt.value,
          },
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
        borderWidth={"1 1 1 1"}
        className="max-w-[1250px]"
      >
        <UtbetalingStatusList utbetaling={utbetaling} />
      </Box>
    </VStack>
  );
}
