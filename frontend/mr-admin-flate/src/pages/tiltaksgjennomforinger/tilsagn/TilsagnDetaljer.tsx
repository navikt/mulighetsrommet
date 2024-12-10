import { useAnnullerTilsagn } from "@/api/tilsagn/useAnnullerTilsagn";
import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { BesluttTilsagnRequest, NavAnsattRolle, TilsagnBesluttelseStatus } from "@mr/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { PencilFillIcon, TrashFillIcon, TrashIcon } from "@navikt/aksel-icons";
import { ActionMenu, Alert, BodyShort, Box, Button, Heading, HStack } from "@navikt/ds-react";
import { useRef, useState } from "react";
import { Link, useLoaderData, useMatch, useNavigate, useParams } from "react-router-dom";
import { useSlettTilsagn } from "../../../api/tilsagn/useSlettTilsagn";
import { TiltakDetaljerForTilsagn } from "../../../components/tilsagn/TiltakDetaljerForTilsagn";
import { AFTTilsagnDetaljer } from "./AFTTilsagnDetaljer";
import { AvvistDetaljer } from "./AvvistDetaljer";
import { AvvisTilsagnModal } from "./AvvisTilsagnModal";
import styles from "./TilsagnDetaljer.module.scss";
import { tilsagnLoader } from "./tilsagnLoader";
import { TilsagnTag } from "./TilsagnTag";

export function TilsagnDetaljer() {
  const { avtaleId, tiltaksgjennomforingId } = useParams();
  const besluttMutation = useBesluttTilsagn();
  const annullerMutation = useAnnullerTilsagn();
  const slettMutation = useSlettTilsagn();
  const navigate = useNavigate();
  const annullerModalRef = useRef<HTMLDialogElement>(null);
  const slettTilsagnModalRef = useRef<HTMLDialogElement>(null);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const { tiltaksgjennomforing, tilsagn, ansatt } = useLoaderData<typeof tilsagnLoader>();

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
      tittel: "Tilsagnsoversikt",
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

  function slettTilsagn() {
    if (tilsagn) {
      slettMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilGjennomforing });
    }
  }

  const visBesluttKnapp =
    !tilsagn?.besluttelse &&
    ansatt?.navIdent !== tilsagn?.opprettetAv &&
    ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER);

  if (!tiltaksgjennomforing || !tilsagn) {
    return <Laster tekst="Laster tilsagn..." />;
  }

  const visHandlingerMeny = tilsagn.besluttelse?.status === TilsagnBesluttelseStatus.AVVIST;
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
        <Box background="bg-default" padding={"5"}>
          <HStack gap="2" justify={"end"}>
            {visHandlingerMeny ? (
              <ActionMenu>
                <ActionMenu.Trigger>
                  <Button variant="primary" size="small">
                    Handlinger
                  </Button>
                </ActionMenu.Trigger>
                <ActionMenu.Content>
                  {tilsagn.besluttelse?.status === TilsagnBesluttelseStatus.AVVIST ? (
                    <ActionMenu.Item icon={<PencilFillIcon />}>
                      <Link className={styles.link_without_underline} to="./rediger-tilsagn">
                        Rediger tilsagn
                      </Link>
                    </ActionMenu.Item>
                  ) : null}
                  {tilsagn.besluttelse?.status === TilsagnBesluttelseStatus.AVVIST ? (
                    <ActionMenu.Item
                      variant="danger"
                      onSelect={() => slettTilsagnModalRef.current?.showModal()}
                      icon={<TrashIcon />}
                    >
                      Slett tilsagn
                    </ActionMenu.Item>
                  ) : null}
                </ActionMenu.Content>
              </ActionMenu>
            ) : null}
          </HStack>

          <TiltakDetaljerForTilsagn tiltaksgjennomforing={tiltaksgjennomforing} />
          <AvvistDetaljer tilsagn={tilsagn} />
          <Box
            borderWidth="2"
            borderColor="border-subtle"
            marginBlock={"4 10"}
            borderRadius={"medium"}
            padding={"2"}
          >
            <AFTTilsagnDetaljer tilsagn={tilsagn} />
            <div>
              {besluttMutation.error ? (
                <BodyShort spacing>
                  <Alert variant="error">Klarte ikke lagre beslutning</Alert>
                </BodyShort>
              ) : null}
              <HStack gap="2" justify={"end"}>
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
              <VarselModal
                headingIconType="warning"
                headingText="Slette tilsagnet?"
                modalRef={slettTilsagnModalRef}
                handleClose={() => slettTilsagnModalRef.current?.close()}
                body={
                  <p>
                    Er du sikker på at du vil slette tilsagnet?
                    <br /> Denne operasjonen kan ikke angres
                  </p>
                }
                primaryButton={
                  <Button variant="danger" onClick={slettTilsagn} icon={<TrashFillIcon />}>
                    Ja, jeg vil slette tilsagnet
                  </Button>
                }
                secondaryButton
                secondaryButtonHandleAction={() => slettTilsagnModalRef.current?.close()}
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
      <Button variant="secondary" size="small" type="button" onClick={onAvvisTilsagn}>
        Send i retur med forklaring
      </Button>
      <Button size="small" type="button" onClick={onGodkjennTilsagn}>
        Godkjenn tilsagn
      </Button>
    </HStack>
  );
}
