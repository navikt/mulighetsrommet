import React, { useState } from 'react';
import { Button, Panel } from '@navikt/ds-react';
import SidemenyAccordion from './SidemenyAccordion';
import './Sidemeny.less';
import Tilbakemeldingsmodal from '../modal/Tilbakemeldingsmodal';
import Lenke from '../lenke/Lenke';

const SidemenyDetaljer = () => {
  const [tilbakemeldingsmodalOpen, setTilbakemeldingsmodalOpen] = useState(false);

  return (
    <>
      <Panel className="tiltakstype-detaljer__sidemeny">
        <Lenke isExternal to="https://www.nav.no">
          Se ekstern nettside
        </Lenke>
        <SidemenyAccordion tittel="Kontaktinfo" isOpen={false}>
          Kontaktinfo
        </SidemenyAccordion>
        <SidemenyAccordion tittel="Dokumenter" isOpen={false}>
          Dokumenter
        </SidemenyAccordion>
        <Panel className="tiltakstype-detaljer__sidemeny__tilbakemelding">
          Har du forslag til forbedringer eller endringer vil vi gjerne at du sier ifra
          <Button onClick={() => setTilbakemeldingsmodalOpen(true)} data-testid="btn_gi-tilbakemelding">
            Gi tilbakemelding
          </Button>
        </Panel>
      </Panel>

      <Tilbakemeldingsmodal
        modalOpen={tilbakemeldingsmodalOpen}
        setModalOpen={() => setTilbakemeldingsmodalOpen(false)}
      />
    </>
  );
};

export default SidemenyDetaljer;
