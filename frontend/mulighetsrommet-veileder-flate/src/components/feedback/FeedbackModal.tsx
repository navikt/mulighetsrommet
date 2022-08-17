import * as React from 'react';
import { useState } from 'react';
import { BodyShort, Button, Detail, Heading, Textarea } from '@navikt/ds-react';
import './Feedback.less';
import FeedbackTilfredshetValg from './FeedbackTilfredshetValg';
import Lenke from '../lenke/Lenke';
import Show from '../../utils/Show';

const FeedbackModal = () => {
  const [tilfredshet, setTilfredshet] = useState(0);

  const harBesvartTilfredshet = tilfredshet > 0;
  console.log(tilfredshet);

  const handleTilfredshetChanged = (tilfredshet: number) => {
    console.log(harBesvartTilfredshet);
    setTilfredshet(tilfredshet);
  };

  return (
    <div className="feedback__modal">
      <div className="feedback__modal__heading">
        <Heading size="large" level="1">
          Tilbakemelding
        </Heading>
        <BodyShort size="small" className="tilbakemelding-modal__ingress">
          Hjelp oss å bygge en bedre tjeneste ved å gi oss dine innspill og tilbakemeldinger. Alle svar er anonyme.
        </BodyShort>
      </div>

      <FeedbackTilfredshetValg
        sporsmal="Hvordan synes du det fungerer med Arbeidsmarkedstiltak her i Modia?"
        onTilfredshetChanged={() => handleTilfredshetChanged(tilfredshet)}
      />

      <Show if={harBesvartTilfredshet}>
        <>
          <div className="feedback__modal__textarea">
            <Textarea
              label="Fortell gjerne mer om hvordan du opplever tjenesten"
              description="Vennligst ikke ta med noen konfidensiell eller personlig informasjon i kommentaren din."
              minRows={4}
              maxLength={750}
            />
            <Button className="feedback__modal__textarea__send-btn">Send</Button>
          </div>

          <Detail>
            Dette er kun for tilbakemelding og overvåkes ikke av support. Trenger du hjelp? Ta kontakt med oss i{' '}
            <Lenke isExternal to="https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401">
              Porten
            </Lenke>
          </Detail>
        </>
      </Show>
    </div>
  );
};

export default FeedbackModal;
