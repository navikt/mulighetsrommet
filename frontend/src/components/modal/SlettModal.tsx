import React from 'react';
import { Modal, ModalProps } from '@navikt/ds-react';
import { Fareknapp, Flatknapp } from 'nav-frontend-knapper';
import './Modal.less';
import { ReactComponent as Delete } from '../../ikoner/Delete.svg';
import { Innholdstittel } from 'nav-frontend-typografi';

interface SlettModalProps extends Omit<ModalProps, 'children'> {
  tittel: string;
  handleDelete: () => void;
}

const SlettModal = ({ tittel, handleDelete, ...others }: SlettModalProps) => {
  return (
    <Modal {...others}>
      <Modal.Content>
        <Innholdstittel className="modal-info-tekst__overskrift">{tittel}</Innholdstittel>
        <div className="rediger-tiltaksvariant__slett-modal__tekst">
          Er du sikker på at du vil slette tiltaksvarianten?
        </div>
        <div className="rediger-tiltaksvariant__slett-modal__knapperad">
          <Fareknapp onClick={handleDelete} data-testid="rediger-tiltaksvariant__slett-modal__knapperad">
            Slett <Delete />
          </Fareknapp>
          <Flatknapp onClick={others.onClose}>Avbryt</Flatknapp>
        </div>
      </Modal.Content>
    </Modal>
  );
};

export default SlettModal;
