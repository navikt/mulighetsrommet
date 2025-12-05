import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { FilePdfIcon } from "@navikt/aksel-icons";
import { Alert, Box, Button, Heading, HStack, Link, Modal, Spacer, VStack } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
  UtbetalingTypeDto,
} from "api-client";
import { useRef } from "react";
import { LoaderFunction, MetaFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { PageHeading } from "~/components/common/PageHeading";
import { DeltakelserTable } from "~/components/deltakelse/DeltakelserTable";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import { getEnvironment } from "~/services/environment";
import { tekster } from "~/tekster";
import { getUtbetalingsdato } from "~/utils/utbetaling";
import { deltakerOversiktLenke, pathByOrgnr } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import css from "../root.module.css";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";

type UtbetalingDetaljerSideData = {
  utbetaling: ArrangorflateUtbetalingDto;
  deltakerlisteUrl: string;
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
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());
  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const [{ data: utbetaling, error: utbetalingError }] = await Promise.all([
    ArrangorflateService.getArrangorflateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError) {
    throw problemDetailResponse(utbetalingError);
  }

  return { utbetaling, deltakerlisteUrl };
};

export default function UtbetalingDetaljerSide() {
  const { utbetaling, deltakerlisteUrl } = useLoaderData<UtbetalingDetaljerSideData>();

  const visNedlastingAvKvittering = [
    ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    ArrangorflateUtbetalingStatus.UTBETALT,
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
      <UtbetalingHeader utbetalingType={utbetaling.type} />
      <Definisjonsliste
        definitions={[
          getUtbetalingsdato(utbetaling),
          { key: "Tiltaksnavn", value: utbetaling.gjennomforing.navn },
          { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
          { key: "Løpenummer", value: utbetaling.gjennomforing.lopenummer },
        ]}
      />
      <Definisjonsliste
        title={"Utbetaling"}
        definitions={[
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
          {
            key: "Utbetales tidligst",
            value: formaterDato(utbetaling.utbetalesTidligstDato) ?? "-",
          },
        ]}
      />
      <SatsPerioderOgBelop
        belop={utbetaling.beregning.belop}
        satsDetaljer={utbetaling.beregning.satsDetaljer}
      />
      <DeltakerModal utbetaling={utbetaling} deltakerlisteUrl={deltakerlisteUrl} />
      <Definisjonsliste
        title="Betalingsinformasjon"
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

function UtbetalingHeader({ utbetalingType }: { utbetalingType: UtbetalingTypeDto }) {
  const tekst = utbetalingType.displayNameLong ?? utbetalingType.displayName;
  return (
    <HStack gap="2">
      <Heading level="3" size="medium">
        {tekst}
      </Heading>
      <UtbetalingTypeTag type={utbetalingType} />
    </HStack>
  );
}

interface DeltakerModalProps {
  utbetaling: ArrangorflateUtbetalingDto;
  deltakerlisteUrl: string;
}

function DeltakerModal({ utbetaling, deltakerlisteUrl }: DeltakerModalProps) {
  const modalRef = useRef<HTMLDialogElement>(null);

  if (!utbetaling.kanViseBeregning) {
    return null;
  }

  return (
    <HStack gap="2">
      <Button variant="secondary" size="small" onClick={() => modalRef.current?.showModal()}>
        Se deltakelser
      </Button>
      <Modal
        ref={modalRef}
        size="medium"
        header={{ heading: utbetaling.beregning.displayName }}
        onClose={() => modalRef.current?.close()}
        width="80rem"
        closeOnBackdropClick
      >
        <Modal.Body>
          <VStack gap="2">
            {utbetaling.beregning.stengt.length > 0 && (
              <Alert variant={"info"}>
                {tekster.bokmal.utbetaling.beregning.stengtHosArrangor}
                <ul>
                  {utbetaling.beregning.stengt.map(({ periode, beskrivelse }) => (
                    <li key={periode.start + periode.slutt}>
                      {formaterPeriode(periode)}: {beskrivelse}
                    </li>
                  ))}
                </ul>
              </Alert>
            )}
            <DeltakelserTable
              beregning={utbetaling.beregning}
              advarsler={utbetaling.advarsler}
              deltakerlisteUrl={deltakerlisteUrl}
            />
            <SatsPerioderOgBelop
              belop={utbetaling.beregning.belop}
              satsDetaljer={utbetaling.beregning.satsDetaljer}
            />
          </VStack>
        </Modal.Body>
      </Modal>
    </HStack>
  );
}
