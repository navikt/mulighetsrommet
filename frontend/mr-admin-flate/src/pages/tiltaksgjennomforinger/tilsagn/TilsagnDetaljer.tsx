import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAnnullerTilsagn } from "@/api/tilsagn/useAnnullerTilsagn";
import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { useTiltaksgjennomforingById } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Bolk } from "@/components/detaljside/Bolk";
import { Header } from "@/components/detaljside/Header";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { formaterDato } from "@/utils/Utils";
import {
  BesluttTilsagnRequest,
  NavAnsattRolle,
  TilsagnBesluttelseStatus,
  TilsagnDto,
} from "@mr/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { TrashFillIcon, TrashIcon } from "@navikt/aksel-icons";
import {
  ActionMenu,
  Alert,
  BodyShort,
  Box,
  Button,
  Heading,
  HStack,
  Tag,
  VStack,
} from "@navikt/ds-react";
import { useRef, useState } from "react";
import { Link, useMatch, useNavigate, useParams } from "react-router-dom";
import { TiltakDetaljerForTilsagn } from "../../../components/tilsagn/TiltakDetaljerForTilsagn";
import { AvvistDetaljer } from "./AvvistDetaljer";
import { AvvisTilsagnModal } from "./AvvisTilsagnModal";
import { useGetTilsagnById } from "./useGetTilsagnById";
import { DetaljerContainer } from "../../DetaljerContainer";

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
      tittel: "Tiltaksgjennomføringdetaljer",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
    {
      tittel: "Tilsagn",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn`,
    },
    {
      tittel: "Tilsagnsdetaljer",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn`,
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
          Tilbake til tilsagnsoversikt
        </Link>
        <Box background="bg-default" padding={"5"}>
          <HStack gap="2" justify={"space-between"}>
            <Heading size="medium" level="2">
              Tilsagn
            </Heading>
            <ActionMenu>
              <ActionMenu.Trigger>
                <Button variant="primary" size="small">
                  Handlinger
                </Button>
              </ActionMenu.Trigger>
              <ActionMenu.Content>
                <ActionMenu.Item
                  onSelect={() =>
                    alert("Redigering av tilsagn her i fra er ikke implementert enda ")
                  }
                >
                  Rediger tilsagn
                </ActionMenu.Item>
                <ActionMenu.Item
                  variant="danger"
                  onSelect={() => alert("Sletting av tilsagn er ikke implementert enda")}
                  icon={<TrashIcon />}
                >
                  Slett tilsagn
                </ActionMenu.Item>
              </ActionMenu.Content>
            </ActionMenu>
          </HStack>
          <Separator />
          <Heading size="small" level="3">
            Tiltaksgjennomføring
          </Heading>
          <TiltakDetaljerForTilsagn tiltaksgjennomforing={tiltaksgjennomforing} borderWidth={"0"} />
          <AvvistDetaljer tilsagn={tilsagn} />
          <Box
            borderWidth="2"
            borderColor="border-subtle"
            marginBlock={"4 10"}
            borderRadius={"medium"}
            padding={"2"}
          >
            <div>
              <HStack justify={"space-between"} align={"baseline"} padding={"5"}>
                <Heading size="medium" level="3" spacing>
                  Tilsagn
                </Heading>
              </HStack>
              <VStack padding="5">
                <Heading size="small" level="4">
                  Periode og plasser
                </Heading>
                <Bolk>
                  <Metadata header="Dato fra" verdi={formaterDato(tilsagn.periodeStart)} />
                  <Metadata header="Dato til" verdi={formaterDato(tilsagn.periodeSlutt)} />
                  <Metadata header="Tilsagnsstatus" verdi={<TilsagnTag tilsagn={tilsagn} />} />
                </Bolk>
                <Bolk>
                  <Metadata
                    header="Antall plasser"
                    verdi={tilsagn.tiltaksgjennomforing.antallPlasser}
                  />
                  <Metadata header="Sats per plass per måned" verdi={"TODO"} />
                </Bolk>
                <Bolk>
                  <Metadata
                    header="Kostnadssted"
                    verdi={`${tilsagn.kostnadssted.enhetsnummer} ${tilsagn.kostnadssted.navn}`}
                  />
                </Bolk>
              </VStack>
            </div>
            <div>
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
                ) : null}
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
            </div>
          </Box>
        </Box>
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
        Send i retur med forklaring
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
