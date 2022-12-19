import { Flex } from "@sanity/ui";
import React from "react";
import styled from "styled-components";

const ToolRoot = styled(Flex)`
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  flex-direction: column;
`;

export function Samtykke(props) {
  return (
    <ToolRoot>
      <div style={{ maxWidth: "65ch" }}>
        <h1>Samtykke i forbindelse med personvern i Sanity</h1>
        <p>
          Ditt personvern er viktig for oss. Derfor skal du være sikker på at du
          alltid kan trekke ditt samtykke uten spørsmål.
        </p>
        <p>
          Ved innlogging til Sanity sendes din IP-adresse, navn og nav-epost til
          Sanity sine servere i USA. Dersom du ønsker å trekke samtykke så gjør
          du følgende:
        </p>
        <ol>
          <li>
            Send en mail til{" "}
            <a href="mailto:Marthe.S.Paulsen.Kathrud@nav.no?subject=Trekke samtykke som redaktør i Sanity&body=Jeg <fullt navn> ønsker å trekke mitt samtykke som redaktør i Sanity.">
              Marthe.S.Paulsen.Kathrud@nav.no
            </a>{" "}
            der du oppgir fullt navn og at du ønsker å trekke ditt samtykke som
            redaktør i Sanity.
          </li>
        </ol>
        <p>
          Her finner du{" "}
          <a
            href="https://navno.sharepoint.com/sites/intranett-hr/SitePages/Personvernerkl%C3%A6ring.aspx?web=1"
            target="_blank"
            rel="noopener noreferrer"
          >
            {" "}
            informasjon om personvern for ansatte i Arbeids- og velferdsetaten
          </a>{" "}
          eller du kan lese mer om personvern hos Sanity på{" "}
          <a
            href="https://www.sanity.io/legal/privacy"
            target="_blank"
            rel="noopener noreferrer"
          >
            Sanitys egne personvernsider
          </a>
          .
        </p>
      </div>
    </ToolRoot>
  );
}
