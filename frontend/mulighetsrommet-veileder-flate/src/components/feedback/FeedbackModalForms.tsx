import * as React from 'react';
import { BodyShort, Detail, Heading } from '@navikt/ds-react';
import styles from './Feedback.module.scss';
import Lenke from '../lenke/Lenke';
import classNames from 'classnames';
import Show from '../../utils/Show';

interface FeedbackModalProps {
  isModalOpen: boolean;
}

const FeedbackModalForms = ({ isModalOpen }: FeedbackModalProps) => {
  return (
    <Show if={isModalOpen}>
      <div
        className={classNames(styles.feedBackModal, {
          [styles.feedBackModal__slideIn]: isModalOpen,
        })}
      >
        <div className={styles.feedBackModal__heading}>
          <Heading size="large" level="1">
            Tilbakemelding
          </Heading>
          <BodyShort size="small">
            Hjelp oss å bygge en bedre tjeneste ved å gi oss dine innspill og tilbakemeldinger. Alle svar er anonyme.
          </BodyShort>
        </div>

        <Lenke
          isExternal
          to="https://forms.office.com/r/gGtRvL8Niv"
          className={classNames(styles.feedBackModal__forms, styles.feedBackModal__textarea)}
        >
          Gi tilbakemelding
        </Lenke>

        <Detail>
          Dette er kun for tilbakemelding og overvåkes ikke av support. Trenger du hjelp? Ta kontakt med oss i{' '}
          <Lenke isExternal to="https://jira.adeo.no/plugins/servlet/desk/portal/541/create/4442">
            Porten
          </Lenke>
        </Detail>
      </div>
    </Show>
  );
};

export default FeedbackModalForms;
