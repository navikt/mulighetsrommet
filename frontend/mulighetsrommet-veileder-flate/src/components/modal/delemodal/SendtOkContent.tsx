import { SuccessColored } from '@navikt/ds-icons';
import { BodyShort, Button, Heading } from '@navikt/ds-react';
import classNames from 'classnames';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { useNavigerTilDialogen } from '../../../hooks/useNavigerTilDialogen';
import modalStyles from '../Modal.module.scss';
import delemodalStyles from './Delemodal.module.scss';
import { State } from './DelemodalActions';

interface Props {
  onCancel: (value: boolean) => void;
  state: State;
}

export function SendtOkContent({ onCancel, state }: Props) {
  const fnr = useHentFnrFraUrl();
  const { navigerTilDialogen } = useNavigerTilDialogen();

  return (
    <div className={delemodalStyles.delemodal_tilbakemelding}>
      <SuccessColored className={delemodalStyles.delemodal_svg} />
      <Heading level="1" size="large" data-testid="modal_header">
        Meldingen er sendt
      </Heading>
      <BodyShort>Du kan fortsette dialogen om dette tiltaket i Dialogen.</BodyShort>
      <div className={classNames(modalStyles.modal_btngroup, modalStyles.modal_btngroup_success)}>
        <Button
          variant="primary"
          onClick={() => navigerTilDialogen(fnr, state.dialogId)}
          data-testid="modal_btn-dialog"
        >
          Gå til Dialogen
        </Button>
        <Button variant="secondary" onClick={() => onCancel(false)} data-testid="modal_btn-cancel">
          Lukk
        </Button>
      </div>
    </div>
  );
}
