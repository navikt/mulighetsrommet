import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import {
  ENDRINGSMELDINGER_URL,
  previewArbeidsmarkedstiltakUrl,
  sanityStudioUrl,
  selectAccountUrl,
} from "@/constants";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { BellIcon, MenuGridIcon } from "@navikt/aksel-icons";
import { Dropdown, Heading, InternalHeader, Modal, ReadMore, Spacer } from "@navikt/ds-react";
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
      <InternalHeader.Title as="h1">
        <Link className="no-underline to-ax-text-accent-contrast" to="/">
          Nav Tiltaksadministrasjon
        </Link>
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
      <Dropdown>
        <InternalHeader.Button as={Dropdown.Toggle}>
          <MenuGridIcon style={{ fontSize: "1.5rem" }} title="Meny" />
        </InternalHeader.Button>

        <Dropdown.Menu>
          <Dropdown.Menu.GroupedList>
            <Dropdown.Menu.GroupedList.Item onClick={() => navigate("/tiltakstyper")}>
              Tiltakstyper
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item onClick={() => navigate("/avtaler")}>
              Avtaler
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item onClick={() => navigate("/gjennomforinger")}>
              Gjennomføringer
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item onClick={() => navigate("/arrangorer")}>
              Arrangører
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item onClick={() => navigate("/innsendingsoversikt")}>
              Manglende innsendinger
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.GroupedList.Item onClick={() => navigate("/oppgaveoversikt/oppgaver")}>
              Oppgaver
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => navigate("/oppgaveoversikt/notifikasjoner")}
            >
              Notifikasjoner
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.GroupedList.Item as="a" href={sanityStudioUrl()} target="_blank">
              Individuelle tiltaksgjennomføringer
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              as="a"
              href={previewArbeidsmarkedstiltakUrl()}
              target="_blank"
            >
              Veilederflate forhåndsvisning
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.GroupedList.Item as="a" href={ENDRINGSMELDINGER_URL} target="_blank">
              Endringsmeldinger
            </Dropdown.Menu.GroupedList.Item>
          </Dropdown.Menu.GroupedList>
          <Dropdown.Menu.Divider />
          <Dropdown.Menu.List>
            <Dropdown.Menu.List.Item as="a" href={selectAccountUrl()}>
              Logg ut
            </Dropdown.Menu.List.Item>
          </Dropdown.Menu.List>
        </Dropdown.Menu>
        <InlineErrorBoundary>
          <Brukernavn />
        </InlineErrorBoundary>
      </Dropdown>
    </InternalHeader>
  );
}

function Brukernavn() {
  const { data: ansatt } = useHentAnsatt();
  const [open, setOpen] = useState(false);

  const ansattNavn = [ansatt.fornavn, ansatt.etternavn].join(" ");

  return (
    <>
      <InternalHeader.User
        onClick={() => setOpen(true)}
        name={ansattNavn}
        description={ansatt.navIdent}
        className="cursor-pointer"
      />
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
    </>
  );
}
