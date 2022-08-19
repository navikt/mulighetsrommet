import * as React from 'react';
import { BodyShort, Button, Detail, Heading } from '@navikt/ds-react';
import './Feedback.less';
import Lenke from '../lenke/Lenke';
import classNames from 'classnames';
import { ExternalLink } from '@navikt/ds-icons';

interface FeedbackModalProps {
  isModalOpen: boolean;
}

const FeedbackModalForms = ({ isModalOpen }: FeedbackModalProps) => {
  return (
    <div
      className={classNames('feedback__modal', {
        'feedback__modal--slide-in': isModalOpen,
      })}
    >
      <div className="feedback__modal__heading">
        <Heading size="large" level="1">
          Tilbakemelding
        </Heading>
        <BodyShort size="small">
          Hjelp oss å bygge en bedre tjeneste ved å gi oss dine innspill og tilbakemeldinger. Alle svar er anonyme.
        </BodyShort>
      </div>

      <Button
        className="feedback__modal__textarea"
        onClick={() => window.open('https://forms.office.com/r/gGtRvL8Niv')}
      >
        Gi tilbakemelding <ExternalLink aria-label="Ekstern lenke" />
      </Button>

      <Detail>
        Dette er kun for tilbakemelding og overvåkes ikke av support. Trenger du hjelp? Ta kontakt med oss i{' '}
        <Lenke isExternal to="https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401">
          Porten
        </Lenke>
      </Detail>
    </div>
  );
};

export default FeedbackModalForms;
