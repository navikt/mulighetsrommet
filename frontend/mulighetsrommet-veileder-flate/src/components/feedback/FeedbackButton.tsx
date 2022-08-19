import React from 'react';
import { Button } from '@navikt/ds-react';
import { Close, DialogReport } from '@navikt/ds-icons';
import './Feedback.less';

interface FeedbackButtonProps {
  handleClick: () => void;
  isModalOpen: boolean;
}

const FeedbackButton = ({ handleClick, isModalOpen }: FeedbackButtonProps) => {
  return (
    <Button
      className="feedback__btn"
      onClick={handleClick}
      title="Hjelp oss å bli bedre ved å dele tilbakemeldingen din."
    >
      {isModalOpen ? <Close className="feedback__btn__icon" /> : <DialogReport className="feedback__btn__icon" />}
    </Button>
  );
};

export default FeedbackButton;
