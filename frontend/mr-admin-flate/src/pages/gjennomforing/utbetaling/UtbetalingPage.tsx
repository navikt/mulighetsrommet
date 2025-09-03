import React from "react";
import { MetadataFritekstfelt, MetadataHorisontal } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { v4 as uuidv4 } from "uuid";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { utbetalingLinjeCompareFn } from "@/utils/Utils";
import {
  DelutbetalingRequest,
  FieldError,
  OpprettDelutbetalingerRequest,
  Rolle,
  TilsagnDto,
  TilsagnStatus,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinje,
  ValidationError,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteFillIcon } from "@navikt/aksel-icons";
import {
  Accordion,
  Alert,
  Button,
  CopyButton,
  Heading,
  HGrid,
  HStack,
  VStack,
} from "@navikt/ds-react";
import { useParams } from "react-router";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { BesluttUtbetalingLinjeView } from "@/components/utbetaling/BesluttUtbetalingLinjeView";
import { RedigerUtbetalingLinjeView } from "@/components/utbetaling/RedigerUtbetalingLinjeView";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useEffect, useState } from "react";
import { UtbetalingTypeText } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import UtbetalingBeregningView from "@/components/utbetaling/beregning/UtbetalingBeregningView";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { useQueryClient } from "@tanstack/react-query";
import MindreBelopModal from "@/components/utbetaling/MindreBelopModal";
import { HarTilgang } from "@/components/auth/HarTilgang";
import {
  useTilsagnTilUtbetaling,
  useUtbetaling,
  useUtbetalingBeregning,
  useUtbetalingEndringshistorikk,
} from "./utbetalingPageLoader";

function useUtbetalingPageData() {
  const { gjennomforingId, utbetalingId } = useParams();
  if (!gjennomforingId || !utbetalingId) {
    throw Error("Fant ikke gjennomforingId eller utbetalingId i url");
  }

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: historikk } = useUtbetalingEndringshistorikk(utbetalingId);
  const { data: utbetaling } = useUtbetaling(utbetalingId);
  const { data: tilsagn } = useTilsagnTilUtbetaling(utbetalingId);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter: [] }, utbetalingId);

  // @todo: This is quickfix. Figure out why it scrolls to the bottom on page load as a part of the broader frontend improvements
  useEffect(() => {
    window.scrollTo(0, 0); // Reset scroll position to the top
  }, []);

  return {
    gjennomforing,
    historikk,
    tilsagn,
    utbetaling: utbetaling.utbetaling,
    handlinger: utbetaling.handlinger,
    linjer: utbetaling.linjer.toSorted(utbetalingLinjeCompareFn),
    beregning,
  };
}

function genrererUtbetalingLinjer(tilsagn: TilsagnDto[]): UtbetalingLinje[] {
  return tilsagn
    .filter((t) => t.status === TilsagnStatus.GODKJENT)
    .map((t) => ({
      belop: 0,
      tilsagn: t,
      gjorOppTilsagn: false,
      id: uuidv4(),
    }))
    .toSorted(utbetalingLinjeCompareFn);
}

export function UtbetalingPage() {
  const { gjennomforingId, utbetalingId } = useParams();
  const { gjennomforing, historikk, tilsagn, utbetaling, linjer, beregning, handlinger } =
    useUtbetalingPageData();
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);
  const [linjerState, setLinjerState] = useState<UtbetalingLinje[]>(() =>
    linjer.length === 0 ? genrererUtbetalingLinjer(tilsagn) : linjer,
  );
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const queryClient = useQueryClient();

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = linjerState.map((linje) => {
      return {
        id: linje.id,
        belop: linje.belop,
        gjorOppTilsagn: linje.gjorOppTilsagn,
        tilsagnId: linje.tilsagn.id,
      };
    });

    const body: OpprettDelutbetalingerRequest = {
      utbetalingId: utbetaling.id,
      delutbetalinger: delutbetalingReq,
      begrunnelseMindreBetalt,
    };

    setErrors([]);

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: ["utbetaling", utbetaling.id],
          refetchType: "all",
        });
      },
      onValidationError: (error: ValidationError) => {
        setErrors(error.errors);
      },
    });
  }

  function utbetalesTotal(): number {
    return linjerState.reduce((acc, d) => acc + d.belop, 0);
  }

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforingId}/utbetalinger`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <title>Utbetalinger</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="2" className="bg-white border-b-2 border-gray-200 p-2">
        <BankNoteFillIcon color="#2AA758" className="w-10 h-10" />
        <Heading size="large" level="1">
          Utbetaling for {gjennomforing.navn}
        </Heading>
      </HStack>
      <ContentBox>
        <WhitePaddedBox>
          <VStack gap="4">
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
            <VStack
              gap="4"
              id="kostnadsfordeling"
              padding="4"
              className="border-gray-300 border-1 rounded-lg"
            >
              <HGrid columns="1fr 1fr 0.25fr">
                <VStack>
                  <Heading size="medium" level="2" spacing data-testid="utbetaling-til-utbetaling">
                    Til utbetaling
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Status"
                      value={<UtbetalingStatusTag status={utbetaling.status} />}
                    />

                    <MetadataHorisontal
                      header="Utbetalingsperiode"
                      value={formaterPeriode(utbetaling.periode)}
                    />
                    {utbetaling.type.tagName && (
                      <MetadataHorisontal
                        header="Type"
                        value={<UtbetalingTypeText type={utbetaling.type} />}
                      />
                    )}
                    <MetadataHorisontal
                      header="Dato innsendt"
                      value={formaterDato(utbetaling.godkjentAvArrangorTidspunkt)}
                    />
                    <MetadataHorisontal header="Innsendt av" value={utbetaling.innsendtAv} />
                    <MetadataHorisontal
                      header={utbetalingTekster.beregning.belop.label}
                      value={formaterNOK(utbetaling.belop)}
                    />
                    {utbetaling.beskrivelse && (
                      <MetadataFritekstfelt
                        header="Begrunnelse for utbetaling"
                        value={utbetaling.beskrivelse}
                      />
                    )}
                    {utbetaling.begrunnelseMindreBetalt && (
                      <MetadataFritekstfelt
                        header="Begrunnelse for mindre utbetalt"
                        value={utbetaling.begrunnelseMindreBetalt}
                      />
                    )}
                  </VStack>
                </VStack>
                <VStack gap="4">
                  <Heading size="medium" level="2">
                    Betalingsinformasjon
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Kontonummer"
                      value={utbetaling.betalingsinformasjon?.kontonummer}
                    />
                    <MetadataHorisontal
                      header="KID (valgfritt)"
                      value={utbetaling.betalingsinformasjon?.kid || "-"}
                    />
                  </VStack>
                  {utbetaling.journalpostId ? (
                    <>
                      <Heading size="medium" level="2">
                        Journalføring
                      </Heading>
                      <VStack gap="2">
                        <MetadataHorisontal
                          header="Journalpost-ID i Gosys"
                          value={
                            <HStack align="center">
                              <CopyButton
                                size="small"
                                variant="action"
                                copyText={utbetaling.journalpostId}
                                title="Kopier journalpost-ID"
                              />
                              {utbetaling.journalpostId}
                            </HStack>
                          }
                        />
                      </VStack>
                    </>
                  ) : null}
                </VStack>
                <HStack justify="end" align="start">
                  <EndringshistorikkPopover>
                    <ViewEndringshistorikk historikk={historikk} />
                  </EndringshistorikkPopover>
                </HStack>
              </HGrid>
              <>
                <Accordion>
                  <Accordion.Item>
                    <Accordion.Header>Beregning - {beregning.heading}</Accordion.Header>
                    <Accordion.Content>
                      {utbetalingId && (
                        <UtbetalingBeregningView
                          utbetalingId={utbetalingId}
                          beregning={beregning}
                        />
                      )}
                    </Accordion.Content>
                  </Accordion.Item>
                </Accordion>
                {tilsagn.every(
                  (t) => ![TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT].includes(t.status),
                ) && (
                  <Alert variant="info">
                    Det finnes ingen godkjente tilsagn for utbetalingsperioden
                  </Alert>
                )}
                <UtbetalingLinjeView
                  utbetaling={utbetaling}
                  tilsagn={tilsagn}
                  linjer={linjerState}
                  setLinjer={setLinjerState}
                />
              </>
              <VStack gap="2">
                <HStack justify="end">
                  {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
                    <Button
                      size="small"
                      type="button"
                      onClick={() => {
                        if (utbetalesTotal() < utbetaling.belop) {
                          setMindreBelopModalOpen(true);
                        } else {
                          sendTilGodkjenning();
                        }
                      }}
                    >
                      Send til attestering
                    </Button>
                  )}
                </HStack>
                <VStack gap="2" align="end">
                  {errors.map((error) => (
                    <Alert variant="error" size="small">
                      {error.detail}
                    </Alert>
                  ))}
                </VStack>
              </VStack>
            </VStack>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
      <MindreBelopModal
        open={mindreBelopModalOpen}
        handleClose={() => setMindreBelopModalOpen(false)}
        onConfirm={() => {
          setMindreBelopModalOpen(false);
          sendTilGodkjenning();
        }}
        begrunnelseOnChange={(e: any) => setBegrunnelseMindreBetalt(e.target.value)}
        belopUtbetaling={utbetalesTotal()}
        belopInnsendt={utbetaling.belop}
      />
    </>
  );
}

interface UtbetalingLinjeViewProps {
  utbetaling: UtbetalingDto;
  tilsagn: TilsagnDto[];
  linjer: UtbetalingLinje[];
  setLinjer: React.Dispatch<React.SetStateAction<UtbetalingLinje[]>>;
}

function UtbetalingLinjeView({ utbetaling, tilsagn, linjer, setLinjer }: UtbetalingLinjeViewProps) {
  switch (utbetaling.status.type) {
    case "VENTER_PA_ARRANGOR":
      return null;
    case "RETURNERT":
    case "KLAR_TIL_BEHANDLING":
      return (
        <HarTilgang rolle={Rolle.SAKSBEHANDLER_OKONOMI}>
          <RedigerUtbetalingLinjeView
            setLinjer={setLinjer}
            tilsagn={tilsagn}
            utbetaling={utbetaling}
            linjer={linjer}
          />
        </HarTilgang>
      );
    case "TIL_ATTESTERING":
    case "OVERFORT_TIL_UTBETALING":
      return (
        <HarTilgang rolle={Rolle.ATTESTANT_UTBETALING}>
          <BesluttUtbetalingLinjeView utbetaling={utbetaling} linjer={linjer} />
        </HarTilgang>
      );
  }
}
