import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAnnullerTilsagn } from "@/api/tilsagn/useAnnullerTilsagn";
import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { useTiltaksgjennomforingById } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Bolk } from "@/components/detaljside/Bolk";
import { Header } from "@/components/detaljside/Header";
import { Metadata } from "@/components/detaljside/Metadata";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { DetaljerContainer } from "@/pages/DetaljerContainer";
import { DetaljerInfoContainer } from "@/pages/DetaljerInfoContainer";
import { formaterDato } from "@/utils/Utils";
import {
  BesluttTilsagnRequest,
  NavAnsattRolle,
  TilsagnAvvisningAarsak,
  TilsagnBesluttelseStatus,
  TilsagnDto,
} from "@mr/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { TrashFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Heading, HGrid, HStack, List, Tag } from "@navikt/ds-react";
import { useRef, useState } from "react";
import { Link, useMatch, useNavigate, useParams } from "react-router-dom";
import { AvvisTilsagnModal } from "./AvvisTilsagnModal";
import { useGetTilsagnById } from "./useGetTilsagnById";

export function TilsagnDetaljer() {
  const { avtaleId, tiltaksgjennomforingId } = useParams();
  const { data: tilsagn } = useGetTilsagnById();
  const besluttMutation = useBesluttTilsagn();
  const annullerMutation = useAnnullerTilsagn();
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const { data: ansatt } = useHentAnsatt();
  const navigate = useNavigate();
  const annullerModalRef = useRef<HTMLDialogElement>(null);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Forside", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtaledetaljer",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Tiltaksgjennomføring",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
    {
      tittel: "Tilsagn",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
  ];

  function navigerTilGjennomforing() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn`);
  }

  function besluttTilsagn(request: BesluttTilsagnRequest) {
    if (tilsagn) {
      besluttMutation.mutate(
        {
          id: tilsagn.id,
          requestBody: {
            ...request,
          },
        },
        {
          onSuccess: navigerTilGjennomforing,
        },
      );
    }
  }

  function annullerTilsagn() {
    if (tilsagn) {
      annullerMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilGjennomforing });
    }
  }

  const visBesluttKnapp =
    !tilsagn?.besluttelse &&
    ansatt?.navIdent !== tilsagn?.opprettetAv &&
    ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER);

  if (!tiltaksgjennomforing || !tilsagn) {
    return <Laster tekst="Laster tilsagn..." />;
  }

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltaksgjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Tilsagn for {tiltaksgjennomforing.navn} <TilsagnTag tilsagn={tilsagn} />
          </HStack>
        </Heading>
      </Header>
      <ContainerLayout>
        <Link to={`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn`}>
          Tilbake til tilsagn
        </Link>
        <DetaljerContainer>
          <DetaljerInfoContainer withBorderRight={false}>
            <Bolk>
              <Metadata header="Tiltaksnavn" verdi={tiltaksgjennomforing.navn} />
              <Metadata
                header="Arrangør"
                verdi={`${tiltaksgjennomforing.arrangor.navn} -  ${tiltaksgjennomforing.arrangor.organisasjonsnummer}`}
              />
            </Bolk>
            <Bolk>
              <Metadata header="Periodestart" verdi={formaterDato(tilsagn.periodeStart)} />
              <Metadata header="Periodeslutt" verdi={formaterDato(tilsagn.periodeSlutt)} />
            </Bolk>
            <Bolk>
              <Metadata
                header="Kostnadssted"
                verdi={`${tilsagn.kostnadssted.navn} - ${tilsagn.kostnadssted.enhetsnummer}`}
              />
              <Metadata header="Beløp" verdi={formaterNOK(tilsagn.beregning.belop)} />
            </Bolk>
            <Bolk>
              <Metadata header="Løpenummer" verdi={tilsagn.lopenummer} />
            </Bolk>
            {besluttMutation.error ? (
              <BodyShort spacing>
                <Alert variant="error">Klarte ikke lagre beslutning</Alert>
              </BodyShort>
            ) : null}
            <HStack gap="2" justify={"space-between"}>
              {visBesluttKnapp ? (
                <GodkjennAvvisTilsagnButtons
                  onGodkjennTilsagn={() =>
                    besluttTilsagn({ besluttelse: TilsagnBesluttelseStatus.GODKJENT })
                  }
                  onAvvisTilsagn={() => setAvvisModalOpen(true)}
                />
              ) : (
                <div></div>
              )}
              {!tilsagn.annullertTidspunkt && !visBesluttKnapp && (
                <Button
                  variant="danger"
                  size="small"
                  onClick={() => annullerModalRef.current?.show()}
                >
                  {tilsagn.besluttelse ? "Annuller" : "Slett"}
                </Button>
              )}
            </HStack>
            <VarselModal
              headingIconType="warning"
              headingText="Annuller tilsagn?"
              modalRef={annullerModalRef}
              handleClose={() => annullerModalRef.current?.close()}
              body={null}
              primaryButton={
                <Button variant="danger" onClick={annullerTilsagn} icon={<TrashFillIcon />}>
                  Ja, jeg vil annullere
                </Button>
              }
              secondaryButton
              secondaryButtonHandleAction={() => annullerModalRef.current?.close()}
            />
            <AvvisTilsagnModal
              open={avvisModalOpen}
              onClose={() => setAvvisModalOpen(false)}
              onConfirm={(validatedData) => besluttTilsagn(validatedData)}
            />
          </DetaljerInfoContainer>

          {tilsagn.besluttelse?.status === TilsagnBesluttelseStatus.AVVIST ? (
            <DetaljerInfoContainer withBorderRight={false}>
              <Alert variant="warning">
                <Heading size="xsmall" level="3">
                  Tilsagnet er ikke godkjent
                </Heading>
                <p>Du må fikse følgende før tilsagnet kan godkjennes:</p>
                <HGrid columns={2} style={{ marginTop: "1rem" }}>
                  <Metadata
                    header={tilsagn?.besluttelse?.aarsaker?.length === 1 ? "Årsak" : "Årsaker"}
                    verdi={
                      <List>
                        {tilsagn?.besluttelse?.aarsaker?.map((aarsak, index) => (
                          <List.Item key={index}>{tilsagnAarsakTilTekst(aarsak)}</List.Item>
                        ))}
                      </List>
                    }
                  />
                  {tilsagn?.besluttelse?.forklaring ? (
                    <Metadata header="Forklaring" verdi={tilsagn?.besluttelse?.forklaring} />
                  ) : null}
                </HGrid>
              </Alert>
            </DetaljerInfoContainer>
          ) : null}
        </DetaljerContainer>
      </ContainerLayout>
    </main>
  );
}

interface GodkjennAvvisButtonProps {
  onGodkjennTilsagn: () => void;
  onAvvisTilsagn: () => void;
}

function GodkjennAvvisTilsagnButtons({
  onGodkjennTilsagn,
  onAvvisTilsagn,
}: GodkjennAvvisButtonProps) {
  return (
    <HStack gap="2">
      <Button size="small" type="button" onClick={onGodkjennTilsagn}>
        Godkjenn tilsagn
      </Button>
      <Button variant="secondary" size="small" type="button" onClick={onAvvisTilsagn}>
        Avvis tilsagn
      </Button>
    </HStack>
  );
}

function TilsagnTag(props: { tilsagn: TilsagnDto }) {
  const { tilsagn } = props;

  if (tilsagn?.besluttelse?.status === TilsagnBesluttelseStatus.GODKJENT) {
    return (
      <Tag variant="success" size="small">
        Godkjent
      </Tag>
    );
  } else if (tilsagn?.besluttelse?.status === TilsagnBesluttelseStatus.AVVIST) {
    return (
      <Tag variant="warning" size="small">
        Returnert
      </Tag>
    );
  } else if (tilsagn?.annullertTidspunkt) {
    return (
      <Tag variant="neutral" size="small">
        Annullert
      </Tag>
    );
  } else {
    return (
      <Tag variant="info" size="small">
        Til beslutning
      </Tag>
    );
  }
}

type TilsagnAarsak =
  | "Feil periode"
  | "Feil antall plasser"
  | "Feil kostnadssted"
  | "Feil beløp"
  | "Annet - Se forklaring";

function tilsagnAarsakTilTekst(aarsak: TilsagnAvvisningAarsak): TilsagnAarsak {
  switch (aarsak) {
    case TilsagnAvvisningAarsak.FEIL_PERIODE:
      return "Feil periode";
    case TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER:
      return "Feil antall plasser";
    case TilsagnAvvisningAarsak.FEIL_KOSTNADSSTED:
      return "Feil kostnadssted";
    case TilsagnAvvisningAarsak.FEIL_BELOP:
      return "Feil beløp";
    case TilsagnAvvisningAarsak.FEIL_ANNET:
      return "Annet - Se forklaring";
  }
}
