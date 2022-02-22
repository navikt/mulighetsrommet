import { Button, Heading, Modal } from '@navikt/ds-react';
import React from 'react';
import './modal.less';

interface SendInformasjonModalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  handleForm?: () => void;
  heading: string;
  className?: string;
  btnText?: string;
  children: React.ReactNode;
}

const StandardModal = ({
  modalOpen,
  setModalOpen,
  handleForm,
  heading,
  className,
  btnText,
  children,
}: SendInformasjonModalProps) => {
  const handleSend = () => {
    handleForm!();
    setModalOpen();
  };

  return (
    <Modal closeButton open={modalOpen} onClose={setModalOpen} className={className!}>
      <Modal.Content>
        <Heading spacing level="1" size="large">
          {heading}
        </Heading>
        {children}
        <div>
          <Button onClick={handleSend}>{btnText || 'Send'}</Button>
          <Button variant="tertiary" onClick={setModalOpen}>
            Avbryt
          </Button>
        </div>
      </Modal.Content>
    </Modal>
  );
};

export default StandardModal;
