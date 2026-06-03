import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import {
  ENDRINGSMELDINGER_URL,
  previewArbeidsmarkedstiltakUrl,
  sanityStudioUrl,
  selectAccountUrl,
} from "@/constants";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { BellIcon, LeaveIcon, MenuGridIcon } from "@navikt/aksel-icons";
import {
  Heading,
  InternalHeader,
  Modal,
  ReadMore,
  Spacer,
  ActionMenu,
  BodyShort,
  Detail,
  Theme,
} from "@navikt/ds-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { Bolk } from "../detaljside/Bolk";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotifications";
import { OppgaveoversiktIkon } from "../ikoner/OppgaveoversiktIkon";
import { Adventslys } from "../hoytid/jul/Adventslys";

export function AdministratorHeader() {
  const navigate = useNavigate();
  const { data: summary } = useNotificationSummary();
  const date = new Date();

  const harUlesteNotifikasjoner = summary.unreadCount > 0;
  return (
    <InternalHeader>
      <InternalHeader.Title as={Link} to="/">
        Nav Tiltaksadministrasjon
      </InternalHeader.Title>
      <Spacer />
      {date.getMonth() === 11 && <Adventslys />}
      <InternalHeader.Button onClick={() => navigate("/oppgaveoversikt/oppgaver")}>
        {harUlesteNotifikasjoner ? (
          <OppgaveoversiktIkon color="white" className="w-5" />
        ) : (
          <BellIcon fontSize={24} title="Notifikasjonsbjelle" />
        )}
      </InternalHeader.Button>
      <ActionMenu>
        <ActionMenu.Trigger>
          <InternalHeader.Button>
            <MenuGridIcon style={{ fontSize: "1.5rem" }} title="Meny" />
          </InternalHeader.Button>
        </ActionMenu.Trigger>
        <Theme theme="light">
          <ActionMenu.Content>
            <ActionMenu.Group label="Navigasjon">
              <ActionMenu.Item onClick={() => navigate("/tiltakstyper")}>
                Tiltakstyper
              </ActionMenu.Item>
              <ActionMenu.Item onClick={() => navigate("/avtaler")}>Avtaler</ActionMenu.Item>
              <ActionMenu.Item onClick={() => navigate("/gjennomforinger")}>
                Gjennomføringer
              </ActionMenu.Item>
              <ActionMenu.Item onClick={() => navigate("/arrangorer")}>Arrangører</ActionMenu.Item>
              <ActionMenu.Item onClick={() => navigate("/innsendingsoversikt")}>
                Manglende innsendinger
              </ActionMenu.Item>
              <ActionMenu.Divider />
              <ActionMenu.Item onClick={() => navigate("/oppgaveoversikt/oppgaver")}>
                Oppgaver
              </ActionMenu.Item>
              <ActionMenu.Item onClick={() => navigate("/oppgaveoversikt/notifikasjoner")}>
                Notifikasjoner
              </ActionMenu.Item>
              <ActionMenu.Divider />
              <ActionMenu.Item as="a" href={sanityStudioUrl()} target="_blank">
                Individuelle tiltaksgjennomføringer
              </ActionMenu.Item>
              <ActionMenu.Item as="a" href={previewArbeidsmarkedstiltakUrl()} target="_blank">
                Veilederflate forhåndsvisning
              </ActionMenu.Item>
              <ActionMenu.Item as="a" href={ENDRINGSMELDINGER_URL} target="_blank">
                Endringsmeldinger
              </ActionMenu.Item>
            </ActionMenu.Group>
          </ActionMenu.Content>
        </Theme>
        <InlineErrorBoundary>
          <Brukernavn />
        </InlineErrorBoundary>
      </ActionMenu>
    </InternalHeader>
  );
}

function Brukernavn() {
  const { data: ansatt } = useHentAnsatt();
  const [open, setOpen] = useState(false);

  const ansattNavn = [ansatt.fornavn, ansatt.etternavn].join(" ");

  return (
    <>
      <ActionMenu>
        <ActionMenu.Trigger>
          <InternalHeader.UserButton name={ansattNavn} description={ansatt.navIdent} />
        </ActionMenu.Trigger>
        <Theme theme="light">
          <ActionMenu.Content align="end">
            <ActionMenu.Label>
              <dl style={{ margin: "0" }}>
                <BodyShort as="dt" size="small">
                  {ansattNavn}
                </BodyShort>
                <Detail as="dd">{ansatt.navIdent}</Detail>
              </dl>
            </ActionMenu.Label>
            <ActionMenu.Group aria-label="Handlinger">
              <ActionMenu.Divider />
              <ActionMenu.Item onSelect={() => setOpen(true)}>Se brukerdata</ActionMenu.Item>
              <ActionMenu.Item as="a" href={selectAccountUrl()}>
                Logg ut <Spacer /> <LeaveIcon aria-hidden fontSize="1.5rem" />
              </ActionMenu.Item>
            </ActionMenu.Group>
          </ActionMenu.Content>
        </Theme>
      </ActionMenu>
      <Theme theme="light">
        <Modal
          open={open}
          onClose={() => setOpen(false)}
          aria-label="Brukerdata"
          className="w-1/2"
          closeOnBackdropClick
        >
          <Modal.Header closeButton>
            <Heading size="medium">Brukerdata</Heading>
          </Modal.Header>
          <Modal.Body>
            <Bolk>
              <MetadataVStack label="Navn" value={ansattNavn} />
              <MetadataVStack label="Navident" value={ansatt.navIdent} />
              <MetadataVStack label="Epost" value={ansatt.epost || "Ikke registrert"} />
              <MetadataVStack label="Mobil" value={ansatt.mobilnummer || "Ikke registrert"} />
            </Bolk>
            <ReadMore header="Roller" defaultOpen>
              <ul>
                {ansatt.roller.map((dto) => (
                  <li key={dto.rolle}>{dto.navn}</li>
                ))}
              </ul>
            </ReadMore>
          </Modal.Body>
        </Modal>
      </Theme>
    </>
  );
}
