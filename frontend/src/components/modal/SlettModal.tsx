import React from 'react';
import './Modal.less';
import { ReactComponent as Delete } from '../../ikoner/Delete.svg';
import { Button, Heading, Modal, ModalProps } from '@navikt/ds-react';

interface SlettModalProps extends Omit<ModalProps, 'children'> {
  tittel: string;
  handleDelete: () => void;
}

const SlettModal = ({ tittel, handleDelete, ...others }: SlettModalProps) => {
  return (
    <Modal {...others}>
      <Modal.Content>
        <Heading level="1" size="large" className="modal-info-tekst__overskrift">
          {tittel}
        </Heading>
        <div className="rediger-tiltaksvariant__slett-modal__tekst">
          Er du sikker på at du vil slette tiltaksvarianten?
        </div>
        <div className="rediger-tiltaksvariant__slett-modal__knapperad">
          <Button variant="danger" onClick={handleDelete} data-testid="rediger-tiltaksvariant__slett-modal__knapperad">
            Slett <Delete />
          </Button>
          <Button variant="secondary" onClick={others.onClose}>
            Avbryt
          </Button>
        </div>
      </Modal.Content>
    </Modal>
  );
};

export default SlettModal;
