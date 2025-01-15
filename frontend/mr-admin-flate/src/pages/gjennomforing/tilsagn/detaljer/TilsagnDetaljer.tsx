import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import {
  BesluttTilsagnRequest,
  NavAnsattRolle,
  TilsagnStatusBesluttelse,
  TilsagnTilAnnulleringRequest,
} from "@mr/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { EraserIcon, PencilFillIcon, TrashFillIcon, TrashIcon } from "@navikt/aksel-icons";
import { ActionMenu, Alert, BodyShort, Box, Button, Heading, HStack } from "@navikt/ds-react";
import { useRef, useState } from "react";
import { Link, useLoaderData, useMatch, useNavigate, useParams } from "react-router";
import { useSlettTilsagn } from "@/api/tilsagn/useSlettTilsagn";
import { TiltakDetaljerForTilsagn } from "@/components/tilsagn/TiltakDetaljerForTilsagn";
import { TilsagnDetaljerForhandsgodkjent } from "./TilsagnDetaljerForhandsgodkjent";
import { AvvisTilsagnModal } from "../AvvisTilsagnModal";
import styles from "../TilsagnDetaljer.module.scss";
import { tilsagnDetaljerLoader } from "./tilsagnDetaljerLoader";
import { TilsagnTag } from "../TilsagnTag";
import { useTilsagnTilAnnullering } from "@/api/tilsagn/useTilsagnTilAnnullering";
import { TilAnnulleringModal } from "../TilAnnulleringModal";
import { AvvistAlert, TilAnnulleringAlert } from "../AarsakerAlert";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import {
  isTilsagnForhandsgodkjent,
  isTilsagnFri,
} from "@/pages/gjennomforing/tilsagn/tilsagnUtils";
import { TilsagnDetaljerFri } from "@/pages/gjennomforing/tilsagn/detaljer/TilsagnDetaljerFri";
import { ContentBox } from "@/layouts/ContentBox";

export function TilsagnDetaljer() {
  const { gjennomforing, tilsagn, ansatt, historikk } =
    useLoaderData<typeof tilsagnDetaljerLoader>();

  const { avtaleId, tiltaksgjennomforingId } = useParams();
  const besluttMutation = useBesluttTilsagn();
  const tilAnnulleringMutation = useTilsagnTilAnnullering();
  const slettMutation = useSlettTilsagn();
  const navigate = useNavigate();
  const [tilAnnulleringModalOpen, setTilAnnulleringModalOpen] = useState<boolean>(false);
  const slettTilsagnModalRef = useRef<HTMLDialogElement>(null);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Gjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtale",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Gjennomføring",
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

  function navigerTilTilsagnTabell() {
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
          onSuccess: navigerTilTilsagnTabell,
        },
      );
    }
  }

  function tilAnnullering(request: TilsagnTilAnnulleringRequest) {
    if (tilsagn) {
      tilAnnulleringMutation.mutate(
        {
          id: tilsagn.id,
          aarsaker: request.aarsaker,
          forklaring: request.forklaring,
        },
        { onSuccess: navigerTilTilsagnTabell },
      );
    }
  }

  function slettTilsagn() {
    if (tilsagn) {
      slettMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilTilsagnTabell });
    }
  }

  const visBesluttKnapp =
    (tilsagn.status.type === "TIL_GODKJENNING" || tilsagn.status.type === "TIL_ANNULLERING") &&
    ansatt?.navIdent !== tilsagn.status.endretAv &&
    ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER);

  const visHandlingerMeny =
    tilsagn.status.type === "RETURNERT" || tilsagn.status.type === "GODKJENT";

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Tilsagn for {gjennomforing.navn} <TilsagnTag status={tilsagn.status} />
          </HStack>
        </Heading>
      </Header>
      <ContentBox>
        <Box background="bg-default" padding={"5"}>
          <HStack gap="2" justify={"end"}>
            <EndringshistorikkPopover>
              <ViewEndringshistorikk historikk={historikk} />
            </EndringshistorikkPopover>
            {visHandlingerMeny ? (
              <ActionMenu>
                <ActionMenu.Trigger>
                  <Button variant="primary" size="small">
                    Handlinger
                  </Button>
                </ActionMenu.Trigger>
                <ActionMenu.Content>
                  {tilsagn.status.type === "RETURNERT" && (
                    <>
                      <ActionMenu.Item icon={<PencilFillIcon />}>
                        <Link className={styles.link_without_underline} to="./rediger-tilsagn">
                          Rediger tilsagn
                        </Link>
                      </ActionMenu.Item>
                      <ActionMenu.Item
                        variant="danger"
                        onSelect={() => slettTilsagnModalRef.current?.showModal()}
                        icon={<TrashIcon />}
                      >
                        Slett tilsagn
                      </ActionMenu.Item>
                    </>
                  )}
                  {tilsagn.status.type === "GODKJENT" && (
                    <>
                      <ActionMenu.Item
                        variant="danger"
                        onSelect={() => setTilAnnulleringModalOpen(true)}
                        icon={<EraserIcon />}
                      >
                        Annuller tilsagn
                      </ActionMenu.Item>
                    </>
                  )}
                </ActionMenu.Content>
              </ActionMenu>
            ) : null}
          </HStack>
          <TiltakDetaljerForTilsagn tiltaksgjennomforing={gjennomforing} />
          {tilsagn.status.type === "RETURNERT" && <AvvistAlert status={tilsagn.status} />}
          {tilsagn.status.type === "TIL_ANNULLERING" && (
            <TilAnnulleringAlert status={tilsagn.status} />
          )}
          <Box
            borderWidth="2"
            borderColor="border-subtle"
            marginBlock={"4 10"}
            borderRadius={"medium"}
            padding={"2"}
          >
            {isTilsagnForhandsgodkjent(tilsagn) && (
              <TilsagnDetaljerForhandsgodkjent tilsagn={tilsagn} />
            )}
            {isTilsagnFri(tilsagn) && <TilsagnDetaljerFri tilsagn={tilsagn} />}
            <div>
              {besluttMutation.error ? (
                <BodyShort spacing>
                  <Alert variant="error">Klarte ikke lagre beslutning</Alert>
                </BodyShort>
              ) : null}
              <HStack gap="2" justify={"end"}>
                {visBesluttKnapp && tilsagn.status.type === "TIL_GODKJENNING" && (
                  <HStack gap="2">
                    <Button
                      variant="secondary"
                      size="small"
                      type="button"
                      onClick={() => setAvvisModalOpen(true)}
                    >
                      Send i retur
                    </Button>
                    <Button
                      size="small"
                      type="button"
                      onClick={() =>
                        besluttTilsagn({ besluttelse: TilsagnStatusBesluttelse.GODKJENT })
                      }
                    >
                      Godkjenn tilsagn
                    </Button>
                  </HStack>
                )}
                {visBesluttKnapp && tilsagn.status.type === "TIL_ANNULLERING" && (
                  <HStack gap="2">
                    <Button
                      variant="secondary"
                      size="small"
                      type="button"
                      onClick={() =>
                        besluttTilsagn({
                          besluttelse: TilsagnStatusBesluttelse.AVVIST,
                          aarsaker: [],
                          forklaring: null,
                        })
                      }
                    >
                      Avslå annullering
                    </Button>
                    <Button
                      size="small"
                      variant="danger"
                      type="button"
                      onClick={() =>
                        besluttTilsagn({ besluttelse: TilsagnStatusBesluttelse.GODKJENT })
                      }
                    >
                      Bekreft annullering
                    </Button>
                  </HStack>
                )}
              </HStack>
              <TilAnnulleringModal
                open={tilAnnulleringModalOpen}
                onClose={() => setTilAnnulleringModalOpen(false)}
                onConfirm={(validatedData) => tilAnnullering(validatedData)}
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
      </ContentBox>
    </main>
  );
}
