import React from 'react';
import { Button } from '@navikt/ds-react';
import { Close, DialogReport } from '@navikt/ds-icons';
import styles from './Feedbackbutton.module.scss';

interface FeedbackButtonProps {
  handleClick: () => void;
  isModalOpen: boolean;
}

const FeedbackButton = ({ handleClick, isModalOpen }: FeedbackButtonProps) => {
  return (
    <Button
      className={styles.feedback_button}
      onClick={handleClick}
      title="Hjelp oss å bli bedre ved å dele tilbakemeldingen din."
    >
      {isModalOpen ? (
        <Close aria-label="Lukk" className={styles.feedback_button_icon} />
      ) : (
        <DialogReport aria-label="Åpne tilbakemeldingsundersøkelse" className={styles.feedback_button_icon} />
      )}
    </Button>
  );
};

export default FeedbackButton;
